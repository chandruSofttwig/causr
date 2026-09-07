# Where Anomalies Are Measured

This project measures “anomalies” in **two different places**:

1. **Per-log-record anomaly score** (stored in `logs_hot` / `logs_cold`)
2. **AI anomaly detection** on aggregated service metrics (stored in `anomalies`)

These are related, but they are not the same signal.

---

## 1) Per-log-record anomaly score (EWMA-based)

### Where
- Class: `com.example.logprocessor.kafka.LogConsumer`
- Method: `processRawEvent(RawLogEvent event)`

### What EWMA means (in this project)

EWMA stands for **Exponential Weighted Moving Average**: a running “expected value” that updates over time, where newer samples have more influence than older ones.

In this service we use EWMA over a simple error signal derived from each log record (error vs non-error) to detect when the current error rate behavior deviates from the recent baseline.

### What gets scored
- A simple `errSignal` derived from the log level:
  - `errSignal = 1f` if `log_level` is `ERROR`
  - otherwise `0f`
- The EWMA key is scoped by:
  - `tenant_id : service_name # environment`

### How
- `LogConsumer` calls `EwmaScorer.score(ewmaKey, errSignal)`
- `EwmaScorer`:
  - loads previous EWMA + stddev from **Redis** key `ewma:{key}`
  - updates EWMA/stddev
  - returns an `anomaly_score` based on z-like distance from the EWMA
  - persists the updated state back to Redis

### How it affects storage (HOT/COLD)
- After `event.anomaly_score` is computed, `LogConsumer` runs:
  - `LogSamplingService.decide(event)`
- `LogSamplingService` can route to:
  - `HOT` when `log.anomaly_score > 0.7f`
  - otherwise to `COLD` (random sample) or `DROP`

### Where it’s stored
- The `event.anomaly_score` value is inserted into:
  - `logs_hot.anomaly_score`
  - `logs_cold.anomaly_score`

### Tenant and multiple service names

Yes—`tenant.id` is the “company/tenant” boundary, and EWMA state is stored **per tenant**.

However, the EWMA state key is **not only `tenant_id`**. It is also separated by:

- `service_name` (from OTLP `service.name`)
- `environment` (from OTLP `deployment.environment`)

So if a tenant has multiple services, each `(tenant_id, service_name, environment)` group has its own EWMA baseline and anomaly score.

---

## 2) AI anomaly detection (IsolationForest + Isolation model)

### Where
- Class: `com.example.logprocessor.streams.ServiceMetricsWindowHandler`
- Method: `handle(Windowed<String> windowedKey, FeatureAccumulator acc)`

### What gets scored
This scoring is **not per single log row**.
It is based on an aggregated window for a `(service_name, environment)` group:

Features come from `FeatureAccumulator`, built in the Kafka Streams topology:
- `log_volume` (total logs in the window)
- `error_volume` and derived `error_rate`
- `p99_latency_ms` (from `MergingDigest` quantile 0.99)
- `unique_error_types` (count of templates/hashes)
- `new_error_types` (computed from Redis templates set with TTL 7 days)
- `silence_flag` and time-of-day sin/cos

### How
- If `ServiceMetricsHistoryChecker` says there’s at least 7 days of history for the group:
  - `ServiceMetricsWindowHandler` builds an AI `FeatureVector`
  - calls the AI service over **gRPC** using `GrpcAnomalyScorerClient`
  - reads:
    - `anomaly_score`
    - `is_anomaly` boolean

### Synthetic data (inside the AI service)

Even when Java calls the AI service over gRPC, the AI service may still train using **synthetic data** as a fallback.

In `log-processor-service/ai_service/app.py`:
- it queries ClickHouse `service_metrics` for feature columns
- if fewer than **20 rows** are returned (or training fails), it generates random feature vectors (`rng.normal(...)`)
- it then fits `RobustScaler` + `IsolationForest` on that synthetic dataset, so scoring can still return values.

### Where results are stored
- It always inserts the window’s aggregate into `service_metrics` including `ai_anomaly_score`.
- If `is_anomaly` is true, it also inserts into:
  - `anomalies` table (`ClickHouse`)
- It then publishes an alert message via Kafka (see `AnomalyAlertPublisher`).

---

## Gating rules (clustering vs anomaly detection)

- **Log clustering (`POST /cluster`)** is done per log record in `LogConsumer` when `app.ai.enable-clustering` is enabled.
  - It does NOT require 7 days of `service_metrics` history.
- **AI anomaly detection over windows (gRPC)** requires 7 days of history in ClickHouse.
  - Java calls gRPC only if `ServiceMetricsHistoryChecker.hasAtLeastSevenDays(...)` returns `true`.
- **Synthetic training data** is an AI-side fallback.
  - Even after the 7-day gate, the AI service may still use synthetic data if it finds <20 training rows.

---

## End-to-end flow (small example)

### Example input log (OTLP record)

Assume an incoming OTLP log record has:
- `tenant.id = 'acme'`
- `service.name = 'payment-service'`
- `deployment.environment = 'prod'`
- `log_level = 'ERROR'`
- `message = 'ERROR timeout while connecting to database'`

### End-to-end path

```mermaid
flowchart TD
  A[External app logs] --> B[OTEL Collector]
  B -->|Kafka protobuf| C[Kafka topic: logs.raw]
  C --> D[LogConsumer parses LogsData -> RawLogEvent]

  D --> E[EWMA anomaly score: EwmaScorer.score()]
  E --> F[LogSamplingService decides HOT/COLD/DROP]
  F -->|HOT| G[Insert into logs_hot (anomaly_score)]
  F -->|COLD| H[Insert into logs_cold (anomaly_score)]
  F -->|DROP| I[Discard]

  D -->|clustering enabled| J[AI service HTTP POST /cluster]
  J --> K[AI returns cluster_id + template]
  K --> L[Store cluster_id/template in event attributes]

  C --> M[Kafka Streams aggregates logs into windows]
  M --> N[ServiceMetricsWindowHandler.handle()]
  N -->|>= 7 days service_metrics?| O[Call AI service gRPC ScoreBatch]
  O --> P[AI returns anomaly_score + is_anomaly]
  P --> Q[Insert into service_metrics (ai_anomaly_score)]
  Q -->|is_anomaly| R[Insert into anomalies + publish alert]
  Q -->|not anomaly| S[No anomalies row]
```

## Summary (how to think about it)

- `logs_hot` / `logs_cold` anomaly score = **EWMA error-rate anomaly score per log record**
- `anomalies` table = **AI anomaly detection per service+environment time window**

