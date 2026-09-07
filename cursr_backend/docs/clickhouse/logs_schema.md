# ClickHouse log tables (dashboard)

Dashboard APIs in `cursr-backend` read from **`observability.logs_hot`**.

## `observability.logs_hot`

| Column        | Type                    | Role                |
|---------------|-------------------------|---------------------|
| timestamp     | DateTime                | Event time          |
| tenant_id     | LowCardinality(String)  | Multi-tenant        |
| service_name  | LowCardinality(String)  | Service             |
| log_level     | LowCardinality(String)  | ERROR / WARN / INFO |
| message       | String                  | Body                |
| trace_id      | String                  | Tracing             |
| span_id       | String                  | Span                |
| duration_ms   | UInt32                  | Latency             |
| anomaly_score | Float32                 | ML / scoring        |
| cluster_id    | LowCardinality(String)  | Error grouping      |
| attributes    | Map(String, String)     | Extra fields (e.g. `http.status_code` for 5xx-aware error rate) |

## `observability.logs_cold`

Same logical model; plain `String` types instead of `LowCardinality` where applicable. Used for longer retention; the MVP dashboard queries **hot** only.

## `observability.spans`

Written by [log-processor-service](../../log-processor-service) from Kafka topic **`traces.raw`** (OTLP `ExportTraceServiceRequest`). Used for **p95/p99**, **error rate**, **RPM**, and **service health** when recent SERVER spans exist (see `DashboardCacheRefreshJob`); otherwise the dashboard uses `logs_hot`.

| Column         | Type                | Role |
|----------------|---------------------|------|
| start_time     | DateTime64(3)       | Span start |
| trace_id       | String              | Trace |
| span_id        | String              | Span |
| parent_span_id | String              | Parent (empty = root) |
| tenant_id      | String              | `tenant.id` resource attr |
| service_name   | String              | `service.name` |
| environment    | String              | `deployment.environment` |
| span_name      | String              | Operation name |
| span_kind      | String              | e.g. `SERVER`, `CLIENT` |
| status_code    | String              | `UNSET` / `OK` / `ERROR` |
| duration_ms    | UInt32              | From OTLP start/end nanos |
| attributes     | Map(String, String) | Merged resource + span attrs |

## Ingest alignment

If [log-processor-service](../../log-processor-service) (or collectors) still write to `observability.logs`, ensure a pipeline or ETL populates **`logs_hot`** so dashboard queries return data.

## Error clustering MV

See [log_clusters_mv.sql](../../../docs/clickhouse/log_clusters_mv.sql) in the repo root `docs/clickhouse/` folder.
