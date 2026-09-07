## `cursr_backend` — Dashboard Backend (BFF)

This document describes the `cursr_backend` service: its responsibilities, runtime dependencies, and how it serves the dashboard UI.

### What this service is

- **Framework**: Spring Boot
- **Role**: Backend-for-Frontend (BFF) for the dashboard UI
- **Responsibilities**:
  - Query ClickHouse for dashboard read models (error-rate, latency, top errors, clusters, anomalies, RCA text)
  - Cache dashboard results in Redis
  - Push realtime dashboard summary frames to UI via STOMP/WebSocket
  - (Optional) bridge anomaly events from Kafka → Redis pub/sub → WebSocket topics

### Key integration principle

The UI (`next-monorepo/apps/web`) should talk only to `cursr_backend`:

- **REST** for snapshots and drill-down queries
- **WebSocket (STOMP)** for realtime updates

### REST endpoints (dashboard)

Base path: `/api/dashboard`

- **`GET /summary`**
  - Aggregated snapshot (`summaryVersion: 2`):
    - **`kpis`**: headline metrics (error rate, p99, RPM, services healthy, time-to-detect) with explicit windows and comparisons where applicable
    - **`serviceHealth`**: per-service table from `logs_hot` (last 5m): `error_percent`, `p99_ms`, `rps`, `status` (`GREEN` / `AMBER` / `RED`)
    - **`topErrors`**: last 30m vs prior 30m counts, `trend` (`up` / `down` / `flat`), `logsQuery` path for drill-down
    - Legacy slices: `errorRate`, `latency`, `risk`, `errorClusters`, `anomalies`, `serviceMetricsRecent`
  - Intended for initial UI hydration and ETag revalidation
- **`GET /kpis`**: same `kpis` object as inside `summary` (lighter poll)
- **`GET /error-rate`**
- **`GET /p95-latency`**
- **`GET /top-errors`** (enriched rows: trend + `logsQuery`, 30m window)
- **`GET /logs`**: `service_name`, `message` (required); optional `minutes` (default 30), `log_level`, `limit` (default 50, max 500)
- **`GET /failure-risk`**
- **`GET /error-clusters`**
- **`GET /trace/{traceId}`** (if present in controller; used for trace drill-down)

The SQL is centralized in:

- `src/main/java/com/cursr/backend/dashboard/DashboardSql.java`

### ClickHouse tables used

Namespace: `observability`

- `logs_hot`: recent/high-value logs (ERROR/WARN, slow, keywords, anomalous)
- `log_clusters`: clustered errors/templates (used for “clusters” cards)
- `service_metrics`: windowed service metrics (derived in `log-processor-service`)
- `anomalies`: AI anomaly windows and RCA (`rca_text`)

### Metric dictionary (windows and meaning)

| Metric | Data window | Comparison | Definition |
|--------|-------------|------------|------------|
| Error rate (global KPI) | Last 5m | Previous 5m | `(ERROR logs + rows with HTTP 5xx in attributes) / all log lines × 100`. Uses `attributes['http.status_code']` when present. Denominator is log-line volume (proxy for traffic). |
| p99 latency (global KPI) | Last 5m | Previous 5m | `quantile(0.99)(duration_ms)` over all `logs_hot` rows in window. Health: `<200` ms healthy, `200–500` degraded, `>500` critical. |
| Requests/min | Last 1m | Previous 1m | `count() / 60` over `logs_hot` (log lines per minute). |
| Service health table | Last 5m | — | Per `service_name`: error %, p99, RPS (`count()/300`). Status: RED if `error>5%` OR `p99>800ms`; else AMBER if `error>1%` OR `p99>300ms`; else GREEN. |
| Services healthy (KPI) | Last 5m | — | `healthy/total` where healthy = GREEN only. Card severity: all green → green; 1–2 non-green → amber; **more than 2** non-green → red. |
| Top errors | Last 30m | Prior 30m | Group by `service_name`, `message`; `trend` from current vs previous bucket counts. |
| Time to detect | Last incident | — | Latest `anomalies` row with `is_anomaly=1`: `dateDiff(second, window_start, created_at)` (pipeline lag). `avgDetectionSeconds` / `avgMttrSeconds` reserved for future use. |

### Redis caching

`DashboardCacheRefreshJob` periodically refreshes:

- error rate (per service + global two-window)
- latency (per service + global p99 two-window)
- RPM (global two-window)
- service health from logs
- last anomaly time-to-detect
- top errors (30m + trend)
- risk
- error clusters
- anomalies
- service metrics recent
- full summary snapshot

It writes a summary frame to STOMP:

- **Topic**: `/topic/dashboard/summary`

### WebSocket (STOMP)

Configured in `cursr_backend` to expose a broker endpoint:

- **Endpoint**: `/ws/anomalies`
- **Topics**:
  - `/topic/dashboard/summary` (periodic snapshot frames)
  - `/topic/anomalies/{tenantId}` (optional streaming anomaly events)

### Kafka + realtime anomaly bridging (optional)

When enabled, `cursr_backend` can:

1. Consume Kafka anomaly events
2. Publish them to Redis pub/sub
3. Fan-out to WebSocket subscribers

This enables low-latency “push” updates in the UI.

### Configuration

Primary config:

- `cursr_backend/src/main/resources/application.yml`

Common settings:

- Kafka: `spring.kafka.bootstrap-servers`
- ClickHouse: `clickhouse.url`, `clickhouse.username`, `clickhouse.password`
- Redis: `spring.data.redis.host`, `spring.data.redis.port`
- WebSocket/CORS: `app.websocket.allowed-origin`, `app.cors.allowed-origin`

### Run (local)

```bash
mvn -f cursr_backend/pom.xml -DskipTests package
java -jar cursr_backend/target/cursr-backend-0.1.0-SNAPSHOT.jar --server.port=8082
```

Health check:

```bash
curl -s http://localhost:8082/actuator/health
```

