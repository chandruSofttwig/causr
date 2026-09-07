## `log-processor-service` — Ingestion, Sampling, Windowing, and Anomalies

This document complements the existing docs under `log-processor-service/docs/` by focusing on the **end-to-end runtime behavior** and the **interfaces** used by other services.

### What this service does

`log-processor-service` is responsible for:

- consuming **OTLP logs** from Kafka (`logs.raw`, protobuf)
- decoding OTLP → internal events
- computing per-log EWMA anomaly score (Redis-backed state)
- optional log clustering (HTTP call to `ai_service`)
- sampling + routing logs into ClickHouse:
  - **hot** (`observability.logs_hot`)
  - **cold** (`observability.logs_cold`)
- aggregating logs into **30-second windows** (Kafka Streams)
- (optionally) calling `ai_service` gRPC to score windows (IsolationForest)
- when a window is anomalous:
  - insert into `observability.anomalies`
  - publish a Kafka message to `anomaly-alerts` for RCA generation (`llm-router-service`)

### Runtime dependencies

- Kafka broker (topics: `logs.raw`, `anomaly-alerts`, plus Kafka Streams internal topics)
- ClickHouse (`observability` database + schema)
- Redis (EWMA state, template sets)
- Optional: `ai_service` (HTTP + gRPC)

### Key components (code map)

- **Kafka consumer** (raw logs):
  - `src/main/java/com/example/logprocessor/kafka/LogConsumer.java`
- **Kafka Streams windowing** (30s):
  - `src/main/java/com/example/logprocessor/streams/KafkaStreamsTopologyConfiguration.java`
  - `src/main/java/com/example/logprocessor/streams/ServiceMetricsWindowHandler.java`
- **ClickHouse write path**:
  - `src/main/java/com/example/logprocessor/clickhouse/ClickHouseService.java`
- **EWMA scoring**:
  - `src/main/java/com/example/logprocessor/scoring/EwmaScorer.java`
- **Anomaly alert publisher**:
  - `src/main/java/com/example/logprocessor/kafka/AnomalyAlertPublisher.java`
- **7-day history gate**:
  - `src/main/java/com/example/logprocessor/streams/ServiceMetricsHistoryChecker.java`

### ClickHouse tables written

- `observability.logs_hot`
- `observability.logs_cold`
- `observability.service_metrics`
- `observability.anomalies`

### Kafka topics

- **Input**:
  - `logs.raw` (OTLP `LogsData` protobuf bytes)
- **Output**:
  - `anomaly-alerts` (JSON string; consumed by `llm-router-service`)
- **Kafka Streams internals**:
  - changelog/repartition topics created automatically by Streams

### HTTP endpoints (observability readbacks)

`log-processor-service` exposes lightweight read endpoints useful for debugging:

- `GET /api/logs/recent`
- `GET /api/anomalies`
- `GET /api/clusters`

### AI anomaly scoring behavior (windowed)

- Window size: **30s**
- Gate: call gRPC scorer only when we have **≥ 7 days** of service metrics history for `(serviceName, environment)`
- Scorer response: `(anomaly_score, is_anomaly)`
- If `is_anomaly == true`:
  - insert anomaly row
  - publish `anomaly-alerts` for RCA generation

### Dev/test switches (optional)

For local end-to-end demo/testing, the service supports dev-only switches under `app.ai.*`
(see `src/main/java/com/example/logprocessor/config/AiProperties.java`).

When enabled, you can bypass the baseline gate and/or force publish anomalies.

### Runbook: common failure modes

- **Kafka unreachable**:
  - Symptoms: `Connection refused`, `Topic ... not present in metadata`
  - Fix: bring up broker and ensure listeners/ports match `spring.kafka.bootstrap-servers`
- **ClickHouse insert failures**:
  - Symptoms: “bad SQL grammar”, timestamp parse errors
  - Fix: validate schema + JDBC driver expectations; test inserts directly against ClickHouse HTTP endpoint

