# Log Processor Service — What We Built

Spring Boot service that consumes **OTLP Logs** and **OTLP Traces** protobuf from Kafka, applies **log sampling**, and writes rows to **ClickHouse** (`logs_hot` / `logs_cold` / `spans`). Micrometer metrics support operations and billing-style counters.

---

## High-level flow

```text
Kafka topic: logs.raw (value = binary OTLP LogsData)
    → LogConsumer (byte[])
    → LogsData.parseFrom(bytes)
    → OtlpLogsMapper (per LogRecord → RawLogEvent)
    → LogSamplingService (HOT / COLD / DROP)
    → ClickHouseService (insert hot or cold table)
```

### Traces (dashboard latency + trace view)

```text
Kafka topic: traces.raw (value = binary OTLP ExportTraceServiceRequest)
    → TraceConsumer
    → OtlpTracesMapper (per Span → RawSpanEvent)
    → ClickHouseService.insertSpan → observability.spans
```

**Runbook**

1. Create Kafka topic **`traces.raw`** (same cluster as `logs.raw`).
2. Apply DDL: [`schema.sql`](../src/main/resources/db/clickhouse/schema.sql) `CREATE TABLE ... spans` in database `observability`.
3. Route OTLP traces to Kafka (typical: **OpenTelemetry Collector** `otlp` receiver → `kafka` exporter with **protobuf** encoding for `ExportTraceServiceRequest`, or a small bridge service).
4. On apps, enable traces (e.g. Java agent: **remove** `otel.traces.exporter=none`, set `otel.traces.exporter=otlp`).
5. Optional: `app.traces.server-spans-only: true` in [`application.yml`](../src/main/resources/application.yml) to store only `SERVER` spans (smaller table; matches dashboard SQL filters).

The **cursr_backend** dashboard cache prefers **span-based** p99 / error rate / RPM / service health when recent **SERVER** spans exist in the last 10 minutes; otherwise it falls back to `logs_hot`.

---

## Tech stack

| Area | Choice |
|------|--------|
| Runtime | Java 17, Spring Boot 3.3 |
| Messaging | Spring Kafka |
| Payload | OpenTelemetry Proto `io.opentelemetry.proto:opentelemetry-proto:1.0.0-alpha` |
| Kafka value deserializer | `ByteArrayDeserializer` (required for protobuf; avoids UTF-8 / U+FFFD parse errors) |
| Storage | ClickHouse via JDBC (`clickhouse-jdbc` 0.4.6) |
| Metrics | Spring Actuator + Micrometer Prometheus |

---

## Package layout

| Package | Contents |
|---------|----------|
| `com.example.logprocessor` | `LogProcessorServiceApplication` |
| `com.example.logprocessor.config` | `ClickHouseConfig`, `ClickHouseProperties` |
| `com.example.logprocessor.model` | `RawLogEvent`, `StorageDecision` |
| `com.example.logprocessor.sampling` | `LogSamplingService`, `RandomSampler`, `ThreadLocalRandomSampler` |
| `com.example.logprocessor.kafka` | `LogConsumer`, `TraceConsumer`, `OtlpLogsMapper`, `OtlpTracesMapper` |
| `com.example.logprocessor.otlp` | `OtlpLogAttributeSupport` (duration + HTTP status alias) |
| `com.example.logprocessor.clickhouse` | `ClickHouseService`, `ClickHouseIdentifiers` (safe table names from config) |
| `com.example.logprocessor.metrics` | `LogProcessingMetrics` |

---

## OTLP → `RawLogEvent`

- Parses **`LogsData`** from each Kafka message (wire-compatible with typical OTLP export payloads that carry `ResourceLogs` on field 1).
- For each `LogRecord`:
  - **service.name** from resource attributes (default `unknown`)
  - **tenant.id** from resource attribute `tenant.id` (default empty)
  - **log_level** from `severity_text`, else `SeverityNumber` enum name
  - **message** from `AnyValue` body (string, primitives, array, kv-list, bytes as UTF-8)
  - **timestamp** from `time_unix_nano` as `Instant` string (UTC)
  - **trace_id** / **span_id** as lowercase hex; empty if invalid, wrong length, or all zeros
  - **cluster_id** from log attribute `log.cluster_id` if present
  - **attributes** = merged resource + log attributes (string-valued entries)
  - **duration_ms** (ClickHouse column): derived from the first matching log/resource attribute (see below); otherwise left unset (inserts as 0 in CH)
  - **anomaly_score**: set in `LogConsumer` after mapping (EWMA on ERROR signal)

### Dashboard-aligned log attributes

Shared logic lives in `com.example.logprocessor.otlp.OtlpLogAttributeSupport` (used by `OtlpLogsMapper` and `OtlpStreamEventExtractor`).

| Column / use | OTLP attribute keys (first match wins) | Notes |
|--------------|----------------------------------------|--------|
| `duration_ms` | `duration_ms`, `http.server.duration`, `http.client.duration` | Values parsed as **milliseconds** (double string, rounded) |
| `duration_ms` | `http.server.request.duration`, `http.client.request.duration` | OpenTelemetry stable: value in **seconds** → stored as ms |
| `attributes['http.status_code']` (backend SQL / 5xx) | `http.status_code` | If missing, copied from `http.response.status_code` when that is set |

Values above are capped at 24h in ms for safety. Exporters that only put status or latency in the log **body** JSON are not parsed; use structured attributes for dashboard queries.

---

## Sampling rules (`LogSamplingService`)

Decision per `RawLogEvent`:

1. **HOT** (→ `logs_hot`): `ERROR` / `WARN`; or `duration_ms > 1000`; or message contains (case-insensitive) `exception`, `timeout`, `error`, `failed`; or `anomaly_score > 0.7`.
2. **COLD** (→ `logs_cold`): none of the above, but random draw `< 10%`.
3. **DROP**: otherwise.

Null-safe on `log_level` and `message`. Randomness is injectable via `RandomSampler` (production: `ThreadLocalRandom`).

---

## ClickHouse

DDL lives in [`src/main/resources/db/clickhouse/schema.sql`](../src/main/resources/db/clickhouse/schema.sql).

| Object | Role |
|--------|------|
| `logs_hot` | High-signal rows; **TTL 30 days**; `ORDER BY (tenant_id, service_name, timestamp, trace_id)` |
| `logs_cold` | Sampled “tail” rows; **TTL 90 days**; same column layout |
| `log_clusters` | `SummingMergeTree` rollup by tenant + cluster + message |
| `log_clusters_mv` | Materialized view from **`logs_hot` only** (cold does not feed it) |

**Important:** create **`logs_cold`** (and the rest of the schema) in the `observability` database before running the app, or cold-tier inserts will fail with `UNKNOWN_TABLE`.

---

## Configuration

[`src/main/resources/application.yml`](../src/main/resources/application.yml):

- `spring.kafka.*` — brokers, consumer group, **`ByteArrayDeserializer`** for values
- `clickhouse.url` — e.g. `jdbc:clickhouse://localhost:8123/observability`
- `clickhouse.table-logs-hot` / `table-logs-cold` — default `logs_hot` / `logs_cold`
- `clickhouse.server-time-zone` / `server-version` — JDBC driver hints
- `management.endpoints.web.exposure.include` — `health`, `prometheus`, `metrics`

---

## Metrics (Micrometer)

| Name | Meaning |
|------|--------|
| `logs.received` | One increment per **log record** successfully parsed and processed |
| `logs.parse.errors` | OTLP protobuf parse failure per Kafka message |
| `logs.stored` (tag `tier=hot` / `cold`) | Rows inserted |
| `logs.dropped` | Sampling drop |
| `logs.store.errors` (tags `tier=hot` / `cold`) | ClickHouse insert failed for that record (other records in the same Kafka message may still succeed) |
| `logs.sampling.rate` | Gauge: `stored / (stored + dropped)` over hot+cold vs dropped |

Prometheus scrape via Actuator as configured.

---

## Tests

- [`LogSamplingServiceTest`](../src/test/java/com/example/logprocessor/sampling/LogSamplingServiceTest.java) — deterministic branches + fixed `RandomSampler`
- [`OtlpLogsMapperTest`](../src/test/java/com/example/logprocessor/kafka/OtlpLogsMapperTest.java) — protobuf builders, trace/span hex, `LogsData` round-trip

Run: `mvn test`

---

## Operational checklist

1. Kafka reachable; topic **`logs.raw`** exists; producers send **binary OTLP `LogsData`** (not JSON strings).
2. ClickHouse reachable; database **`observability`** exists; run **`schema.sql`** (at minimum `logs_hot` and **`logs_cold`**).
3. Start the app; verify `/actuator/prometheus` and ClickHouse row counts.

---

## Maven coordinates (protobuf)

```xml
<dependency>
  <groupId>io.opentelemetry.proto</groupId>
  <artifactId>opentelemetry-proto</artifactId>
  <version>1.0.0-alpha</version>
</dependency>
```

If this version disagrees with your collector’s OTLP proto generation, align the artifact version with your pipeline.
docker build -t log-processor-ai ./ai_service

docker run -d --name log-processor-ai \
  -p 8000:8000 -p 50051:50051 \
  --env CH_HOST=clickhouse \
  log-processor-ai