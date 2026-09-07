# Log Processor Service — Project Overview

This service turns raw OTLP Log payloads arriving on Kafka into structured rows in ClickHouse, while preserving enough signal for downstream dashboards and anomaly detection.

## High-level data flow

1. **Kafka input**
   - Topic: `logs.raw`
   - Message value: binary OTLP protobuf `LogsData`
2. **Parsing + mapping**
   - `LogConsumer` receives `byte[]` and decodes `LogsData`
   - `OtlpLogsMapper` converts each `LogRecord` into a `RawLogEvent`:
     - `service_name`, `tenant_id`, `log_level`, `message`, `timestamp`
     - trace/span ids in lowercase hex when present/valid
     - merges resource + log attributes into `attributes: Map<String,String>`
3. **Sampling + routing**
   - `LogSamplingService` decides for each event: `HOT`, `COLD`, or `DROP`
   - Hot/cold events are inserted into ClickHouse:
     - `logs_hot` (high signal, tighter retention)
     - `logs_cold` (lower-volume sampled tail)
4. **Rollups + operational metrics**
   - ClickHouse materialized view `log_clusters_mv` rolls up clusters from `logs_hot`
   - Kafka Streams pipeline generates `service_metrics` windows for AI scoring
5. **Anomaly scoring (optional at runtime)**
   - `GrpcAnomalyScorerClient` calls the AI service over gRPC when enough history exists
   - Anomalies are written to `anomalies` and published to a Kafka topic for alerts.

## ClickHouse model

Schema is defined in `src/main/resources/db/clickhouse/schema.sql` and should be executed against the `observability` database.

Key tables:

- `logs_hot`: high-signal rows, 30-day TTL
- `logs_cold`: sampled tail rows, 90-day TTL
- `log_clusters`: rollup of representative messages (derived from `logs_hot`)
- `service_metrics`: time-windowed aggregates used for anomaly scoring
- `anomalies`: anomaly results + feature snapshot (feature JSON)

## REST API surface

The service provides lightweight query endpoints backed by ClickHouse:

- `GET /api/logs/recent`: last 100 rows from `logs_hot`
- `GET /api/search?q=...`: message search in `logs_hot` (ILike pattern)
- `GET /api/clusters`: top clusters by `event_count`
- `GET /api/anomalies`: anomalies ordered by score + time window

## Observability

- Actuator health at `GET /actuator/health`
- Prometheus scrape at `GET /actuator/prometheus`
- Additional metrics are emitted via Micrometer (e.g., received/parsed/stored/dropped).

For the detailed implementation notes, sampling rules, and operational checklist, see:

- `docs/LOG_PROCESSOR_SERVICE.md`

