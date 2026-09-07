# Causr — Development Work Summary

This document summarizes the engineering work completed on the Causr / Cursr observability platform: what was built, fixed, optimized, and enhanced during active development (June 2026).

For architecture reference and API details, see [PROJECT.md](PROJECT.md). For quick start, see [README.md](README.md).

---

## Executive summary

Causr is a monorepo observability platform that ingests OpenTelemetry logs, stores them in ClickHouse, detects anomalies with AI scoring, and serves a developer dashboard with optional Slack alerting.

**Major outcomes from this development cycle:**

| Area | Outcome |
|------|---------|
| **Infrastructure** | Root `docker-compose.yml` with Kafka, Redis, ClickHouse, OTel Collector, and AI scorer |
| **Data pipeline** | Fixed OTLP mapping so dashboards show per-service metrics, latency, and error clusters |
| **Dashboard UI** | New `causr-dashboard` (Vite/React) with Dashboard, Logs, and enriched Anomalies pages |
| **Anomaly detection** | End-to-end flow from Kafka Streams → gRPC AI → ClickHouse + Kafka alerts |
| **Slack alerts** | BFF consumes `anomaly-alerts` and posts formatted Block Kit messages with dedupe |
| **Reliability** | Multiple runtime fixes (CORS, SQL aliases, Redis/Kafka fallbacks, collector restarts) |

---

## Architecture (current state)

```
log-sender-backend (:8082)
  → OTel Collector (:4318)
  → Kafka logs.raw
  → log-processor-service (:8080)
      → ClickHouse (logs_hot, anomalies, log_clusters, service_metrics)
      → Kafka anomaly-alerts
  → cursr_backend (:8090)
      → Redis cache + REST/WebSocket
      → Slack webhook (optional)
  → causr-dashboard (:5173)
```

**Supporting services (Docker):** Zookeeper, Kafka, Redis, ClickHouse, `ai-service` (gRPC :50051, HTTP :8000).

---

## 1. Infrastructure & local dev stack

### Added

- **Root [`docker-compose.yml`](docker-compose.yml)** — Zookeeper, Kafka, Redis, ClickHouse, OTel Collector, topic/schema init jobs, and **`ai-service`** for IsolationForest anomaly scoring.
- **[`infra/kafka/create-topics.sh`](infra/kafka/create-topics.sh)** — `logs.raw` (3 partitions), `traces.raw` (3 partitions), `anomaly-alerts`, `logs.anomalies`.
- **[`infra/clickhouse/init.sql`](infra/clickhouse/init.sql)** — Full `observability` schema: `logs_hot`, `logs_cold`, `log_clusters` + MV, `service_metrics`, `anomalies`, `spans_hot`.
- **[`infra/otel-collector/config.yaml`](infra/otel-collector/config.yaml)** — OTLP receivers → Kafka export (`otlp_proto` encoding).
- **[`infra/clickhouse/users.d/default-user.xml`](infra/clickhouse/users.d/default-user.xml)** — Empty password for ClickHouse 24.3 local dev.
- **[`.env.example`](.env.example)** — Port overrides, `SLACK_WEBHOOK_URL`, AI service ports.

### Aligned configuration

All Spring Boot apps use consistent localhost endpoints:

| Service | Host endpoint |
|---------|---------------|
| Kafka | `localhost:29092` |
| Redis | `localhost:6379` |
| ClickHouse | `localhost:8123/observability` |
| OTel Collector | `localhost:4317` (gRPC), `localhost:4318` (HTTP) |
| AI scorer gRPC | `localhost:50051` |

### Infra fixes

| Issue | Fix |
|-------|-----|
| Zookeeper healthcheck failing | Switched probe from `ruok` to `srvr` |
| OTel Collector Kafka export error | Flat `topic`/`encoding` format for collector v0.96.0 |
| Collector container exiting | Documented `docker compose up -d otel-collector` as first check when ingest stops |

---

## 2. Log processor — ingest & mapping optimizations

### Problem

The dashboard showed misleading data: everything bucketed under `log-sender-backend`, `p99LatencyMs: 0`, empty top errors, useless clusters, and inflated error rates.

### Root causes & fixes

| Symptom | Root cause | Fix |
|---------|------------|-----|
| Single `log-sender-backend` bucket | `OtlpLogsMapper` used **resource** `service.name`, ignoring per-log attributes | Log-record `service.name` and `deployment.environment` now override resource via `firstNonBlank()` |
| `p99LatencyMs: 0` | `app.latency_ms` not mapped to `duration_ms` | Added `app.latency_ms` to duration keys in `OtlpLogAttributeSupport` |
| `environment: unknown` | Environment read from resource only | Same override logic as service name |
| Clusters `event_count: 1` | MV hashed full message bodies; no `service_name` | Added `service_name` column; stable `cluster_id` from `issue.type` |
| `log_level` as `SEVERITY_NUMBER_*` | OTLP `severityText` empty, enum name used as fallback | Known cosmetic issue — optional normalization pending |

### Files changed

- `log-processor-service/.../OtlpLogsMapper.java`
- `log-processor-service/.../OtlpLogAttributeSupport.java`
- `log-processor-service/.../OtlpLogsMapperTest.java`
- `infra/clickhouse/init.sql` — `log_clusters` schema + `log_clusters_mv`

### Verified after fix

| Metric | Before | After |
|--------|--------|-------|
| Service health | `log-sender-backend` only | Per-microservice (`payment-service`, `auth-service`, etc.) |
| P99 latency | 0 | ~1200 ms |
| Top errors | Empty | Populated (30m window) |
| Error clusters | Empty `service_name`, count 1 | e.g. `api-gateway` / `dependency_timeout` with aggregated counts |
| `duration_ms` in ClickHouse | Missing | Populated (e.g. 1158, 1074 ms) |

---

## 3. Dashboard BFF (`cursr_backend`) — query & cache enhancements

### Redis cache with ClickHouse fallback

`DashboardQueryService.cachedListOrQuery()` was added so the BFF does not return empty slices when Redis is cold. JDBC fallback applies to:

- Error rate
- Latency (P99)
- Top errors
- Service health
- Error clusters
- Service metrics
- Anomalies

### SQL fixes (`DashboardSql.java`)

| Query | Fix |
|-------|-----|
| `SERVICE_METRICS_RECENT_PER_SERVICE` | Fixed alias shadowing causing `500` on `/api/dashboard/summary` |
| `TOP_ERRORS_30M_WITH_TREND` | Aligned error predicate with error-rate logic (`ERROR` OR HTTP ≥ 500) |
| `TOP_ERROR_CLUSTERS` | Selects `service_name`, uses `sum(event_count)` with `GROUP BY` |
| `TIME_TO_DETECT_LAST_INCIDENT` | Fixed `created_at` alias shadowing in `dateDiff` (qualified with table alias `a.`) |

### Runtime fixes

| Issue | Fix |
|-------|-----|
| `500` on `/api/dashboard/summary` | SQL alias fixes above |
| `403` on `/api/logs/recent` (from browser) | `CorsConfig` in log-processor for `/api/**` |
| BFF startup failure when Redis/Kafka down | Documented dependency order; `kafka-consumer-enabled` can be disabled |
| Kafka anomaly listener | `AnomalyKafkaListener` on topic `anomaly-alerts` with flexible `tenantId` parsing |

### Tests

- `DashboardClickHouseSqlIntegrationTest` — cluster `service_name` assertions
- `ClickHouseDashboardTestSupport` — updated seed data for new cluster schema

---

## 4. Anomaly detection — end-to-end pipeline

### Flow

```
30s Kafka Streams windows (log-processor)
  → feature extraction (error rate, latency, volume, etc.)
  → ai-service gRPC IsolationForest scoring
  → observability.anomalies (ClickHouse)
  → Kafka anomaly-alerts
  → cursr_backend (Redis fan-out + WebSocket + Slack)
```

### Dev profile (`application-dev.yml`)

```yaml
app:
  ai:
    bypass-history-gate: true   # Skip 7-day service_metrics history requirement
    dev-endpoints-enabled: true # Enables POST /api/dev/emit-anomaly
```

Without the dev profile, AI scoring waits for **7 days** of `service_metrics` history before publishing anomalies.

### AI service in Docker

- Added `ai-service` container to `docker-compose.yml`
- Ports: **8000** (HTTP), **50051** (gRPC)
- `GrpcAnomalyScorerClient` logs WARN on gRPC scoring failures

### Dev smoke test

```bash
curl -X POST 'http://localhost:8080/api/dev/emit-anomaly?serviceName=payment-service&environment=staging'
```

Inserts a row into `observability.anomalies` and publishes to `anomaly-alerts`.

### Detection layers

1. **Rule/window layer** — Kafka Streams aggregates per-service metrics in 30s windows.
2. **AI layer** — `ai-service` scores feature vectors; negative scores indicate anomalies.
3. **Alert bus** — Kafka `anomaly-alerts` consumed by BFF for real-time UI and Slack.

---

## 5. Slack alerting

### Design

- **Scope:** Anomaly alerts only — raw log lines are **not** sent to Slack.
- **Trigger:** `AnomalyKafkaListener` consumes `anomaly-alerts`, then calls `AnomalySlackNotifier`.
- **Dedupe:** One message per `(tenant, service, environment)` every 5 minutes (Redis `slack:dedupe:*` keys).
- **Format:** Slack Block Kit — header, score/tenant/window fields, optional error rate, dashboard link.

### Components added (`cursr_backend`)

| File | Role |
|------|------|
| `SlackProperties` | `app.slack.*` configuration |
| `SlackConfig` | Enables `@ConfigurationProperties` |
| `SlackWebhookClient` | POST JSON to Incoming Webhook |
| `SlackAnomalyMessageBuilder` | Builds Block Kit payload |
| `AnomalyDedupeCache` | Redis SET NX with TTL |
| `AnomalySlackNotifier` | Orchestrates dedupe + send |
| `AnomalyKafkaListener` | Wired to call notifier after Redis fan-out |

### Configuration

| Property | Default | Description |
|----------|---------|-------------|
| `app.slack.enabled` | `false` | Master switch |
| `app.slack.webhook-url` | `${SLACK_WEBHOOK_URL}` | Incoming Webhook URL |
| `app.slack.dashboard-base-url` | `http://localhost:5173` | Link in Slack message |
| `app.slack.dedupe-window-minutes` | `5` | Suppress repeat alerts |

### How to enable (correct JVM flag)

Maven `-Dapp.slack.enabled=true` alone does **not** pass the flag to the Spring Boot JVM. Use:

```bash
export SLACK_WEBHOOK_URL=https://hooks.slack.com/services/...
cd cursr_backend
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dapp.slack.enabled=true"
```

### Verified

- Slack webhook returns `HTTP 200` for test payloads.
- Kafka consumer lag on `cursr-backend-anomalies` stays at **0**.
- When `app.slack.enabled` is correctly set, BFF logs: `Slack anomaly alert sent for ...`.

### Unit tests

- `SlackAnomalyMessageBuilderTest`
- `AnomalySlackNotifierTest`
- `AnomalyKafkaListenerTest`

---

## 6. Frontend — `causr-dashboard`

### Built from scratch

Vite + React 19 + TypeScript developer UI at **http://localhost:5173**.

| Page | Data source | Endpoint |
|------|-------------|----------|
| **Dashboard** | BFF | `/api/dashboard/summary` |
| **Logs** | Log processor | `/api/logs/recent` |
| **Anomalies** | BFF | `/api/dashboard/summary` (anomalies slice) + detail APIs |

### UX enhancements

- **Branding** — Logo from `src/assets/logo.png` in sidebar (169×64, centered).
- **Navigation icons** — SVG icons for Dashboard, Logs, Anomalies (`components/icons.tsx`).
- **Dark dev-tool aesthetic** — KPI strip, data tables, status badges, error banners.

### Anomalies page (enriched)

- Expandable rows with anomaly detail panel.
- `AnomalyScoreBadge` — visual score indicator.
- Feature metrics parsed from `featureJson`.
- Related logs via BFF APIs (`fetchAnomalyDetail`, `fetchAnomalyLogs` in `api/bff.ts`).
- Utility helpers in `utils/anomaly.ts`.

### Dashboard page

- **Recent anomalies (1h)** panel added alongside existing KPIs.

### Environment

```bash
VITE_BFF_API_BASE=http://localhost:8090      # default
VITE_PROCESSOR_API_BASE=http://localhost:8080  # default
```

---

## 7. Diagnostics & operational findings

### Log processor health check

| Check | Healthy signal |
|-------|----------------|
| Health endpoint | `GET :8080/actuator/health` → `UP` |
| Kafka consumer lag | `0` on all `logs.raw` partitions |
| ClickHouse ingest | New rows in `logs_hot` within last 2 minutes |
| Service names | Per-microservice, not all `log-sender-backend` |

### Common failure modes

| Symptom | Likely cause | Resolution |
|---------|--------------|------------|
| No new logs in ClickHouse | `otel-collector` container exited | `docker compose up -d otel-collector` |
| Dashboard empty, Redis cold | Cache not warmed yet | BFF JDBC fallback (fixed) or wait for cache refresh job |
| Anomalies empty | AI service down or no dev profile | `docker compose up -d ai-service` + `SPRING_PROFILES_ACTIVE=dev` |
| No Slack messages | `app.slack.enabled` not in JVM | Use `spring-boot.run.jvmArguments` (see above) |
| Port 8080/8090 in use | Orphan JVM from background starts | `ss -tlnp \| grep :8080` then `kill <pid>` |

---

## 8. Testing coverage added

```bash
# Log processor
cd log-processor-service && mvn test
# OtlpLogsMapperTest, OtlpLogAttributeSupportTest, etc.

# BFF
cd cursr_backend && mvn test
# Dashboard SQL integration, Slack components, anomaly listener

# Frontend build
cd causr-dashboard && npm run build
```

`DashboardClickHouseSqlIntegrationTest` uses Testcontainers (requires Docker API 1.40+).

---

## 9. Recommended local run order

```bash
# 1. Infrastructure (including AI scorer)
docker compose up -d

# 2. Log processor (dev profile for local anomaly scoring)
cd log-processor-service && SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run

# 3. Synthetic telemetry
cd log-sender-backend && mvn spring-boot:run

# 4. Dashboard BFF (optional Slack)
export SLACK_WEBHOOK_URL=https://hooks.slack.com/services/...
cd cursr_backend && mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dapp.slack.enabled=true"

# 5. Developer UI
cd causr-dashboard && npm install && npm run dev
```

### Smoke tests

```bash
curl http://localhost:8080/api/logs/recent
curl http://localhost:8090/api/dashboard/summary
curl -X POST 'http://localhost:8080/api/dev/emit-anomaly?serviceName=payment-service&environment=staging'
```

---

## 10. Known gaps & optional follow-ups

| Item | Status |
|------|--------|
| Full OAuth/SSO | Deferred |
| Multi-region HA | Deferred |
| RCA (`llm-router-service`) | Implemented |
| WebSocket live UI | Implemented |
| Traces UI | Implemented |
| API key + tenant isolation | Implemented |
| Docker apps profile + CI | Implemented |

---

## 11. Key files reference

| Area | Paths |
|------|-------|
| **Ingest mapping** | `log-processor-service/.../OtlpLogsMapper.java`, `OtlpLogAttributeSupport.java` |
| **Dashboard SQL** | `cursr_backend/.../DashboardSql.java`, `DashboardQueryService.java` |
| **Slack** | `cursr_backend/.../slack/*`, `anomaly/AnomalySlackNotifier.java` |
| **Anomaly dev** | `log-processor-service/.../DevAnomalyController.java`, `application-dev.yml` |
| **ClickHouse schema** | `infra/clickhouse/init.sql` |
| **Frontend** | `causr-dashboard/src/pages/*`, `components/*`, `api/*` |
| **Infra** | `docker-compose.yml`, `infra/otel-collector/`, `infra/kafka/` |

---

## 12. Documentation updated

- **[README.md](README.md)** — Quick start, Kafka topics, E2E anomaly + Slack smoke test.
- **[PROJECT.md](PROJECT.md)** — Full architecture, API reference, troubleshooting, work-completed section.
- **[.env.example](.env.example)** — Slack webhook and port overrides.

---

*Last updated: June 2026*
