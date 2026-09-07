# Causr / Cursr Observability Platform

Monorepo for an observability and incident-intelligence platform: OTLP telemetry flows through Kafka into ClickHouse, with AI-based anomaly detection and dashboard APIs.

**Setup (start here):** [docs/SETUP.md](docs/SETUP.md)  
**Simplify further (plan sheet):** [docs/SIMPLIFY_SETUP_PLAN.md](docs/SIMPLIFY_SETUP_PLAN.md)  
**Full project guide:** [PROJECT.md](PROJECT.md) — architecture, APIs, data model, and recent work.  
**Instrument real services:** [docs/INSTRUMENTATION.md](docs/INSTRUMENTATION.md)

## Repository layout

| Directory | Role | Port |
|-----------|------|------|
| `log-sender-backend` | Synthetic OTLP log producer | 8082 |
| `log-processor-service` | Kafka ingest, ClickHouse writer, anomaly detection | 8080 |
| `cursr_backend` | Production dashboard BFF (REST + WebSocket) | 8090 |
| `causr-dashboard` | Developer dashboard UI (Vite React) — **primary** | 5173 |
| `observability-dashboard` | Legacy Next.js UI (optional; prefer `causr-dashboard`) | 3000 |
| `cursr_landing` | Marketing landing page (Vite) | 5173 |
| `llm-router-service` | Kafka → Claude RCA → ClickHouse `rca_text` | 8091 |

## Quick start: infrastructure

Start Redis, Kafka (with topics), ClickHouse (with schema), OpenTelemetry Collector, and **AI anomaly scorer**:

```bash
docker compose up -d
# Full stack including Spring apps + dashboard:
# docker compose --profile apps up -d --build
```

Includes `ai-service` on ports **8000** (HTTP clustering) and **50051** (gRPC scoring).

API auth (local default): header `X-API-Key: causr-local-key` (override with `APP_SECURITY_API_KEY`). See [docs/INSTRUMENTATION.md](docs/INSTRUMENTATION.md).

### Verify

```bash
# ClickHouse
curl http://localhost:8123/ping

# Kafka topics (logs.raw, traces.raw, anomaly-alerts, logs.anomalies)
docker exec kafka kafka-topics --bootstrap-server localhost:9092 --list

# ClickHouse tables
curl 'http://localhost:8123/?query=SHOW+TABLES+FROM+observability'

# OTel Collector HTTP (connection refused = not running; 404/405 = running)
curl -s -o /dev/null -w "%{http_code}" http://localhost:4318/v1/logs
```

### Kafka topics

| Topic | Partitions | Purpose |
|-------|------------|---------|
| `logs.raw` | 3 | OTLP logs (protobuf) from OTel Collector |
| `traces.raw` | 3 | OTLP traces (protobuf) from OTel Collector |
| `anomaly-alerts` | 1 | Anomaly events from log-processor-service |
| `logs.anomalies` | 1 | Legacy topic (created for compatibility) |

Host JVM apps connect to Kafka at **`localhost:29092`**. Containers use **`kafka:9092`**.

## Quick start: applications

After infrastructure is healthy, start services in order:

```bash
# 1. Log processor (use dev profile for local AI anomaly scoring)
cd log-processor-service && SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run

# 2. Log sender (OTLP → collector → Kafka → ClickHouse)
cd log-sender-backend && mvn spring-boot:run

# 3. Dashboard BFF (enable Slack via JVM args — Maven -D alone does not reach the app)
cd cursr_backend && mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dapp.slack.enabled=true"

# 3b. RCA worker (optional — needs ANTHROPIC_API_KEY)
cd llm-router-service && mvn spring-boot:run

# 4. Developer dashboard UI (recommended)
cd causr-dashboard && npm install && npm run dev

# Optional: legacy Next.js UI
# cd observability-dashboard && npm run dev
```

### causr-dashboard

Dense dark-themed developer UI with three pages:

| Page | Backend | Port |
|------|---------|------|
| Dashboard | `cursr_backend` | 8090 |
| Logs | `log-processor-service` | 8080 |
| Anomalies | `cursr_backend` | 8090 |

```bash
cd causr-dashboard
cp .env.example .env   # optional — defaults work for local Docker stack
npm install
npm run dev
```

Open [http://localhost:5173](http://localhost:5173). Requires Docker infra plus `log-processor-service`, `log-sender-backend`, and `cursr_backend` running.

Environment variables:

- `VITE_BFF_API_BASE` — default `http://localhost:8090`
- `VITE_PROCESSOR_API_BASE` — default `http://localhost:8080`
- `VITE_BFF_WS_URL` — default `ws://localhost:8090/ws/anomalies`
- `VITE_API_KEY` — must match `APP_SECURITY_API_KEY` (default `causr-local-key`)
- `VITE_TENANT_ID` — default `default`

### End-to-end anomaly detection + Slack

**Detection flow:** 30s Kafka Streams windows → `ai-service` gRPC (IsolationForest) → `observability.anomalies` + Kafka `anomaly-alerts` → `cursr_backend` → Redis/WebSocket + optional Slack.

Use the **`dev` Spring profile** on log-processor so AI scoring runs without 7 days of `service_metrics` history:

```bash
cd log-processor-service && SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run
```

**Slack Incoming Webhook** (alert-only, no RCA):

1. Create an [Incoming Webhook](https://api.slack.com/messaging/webhooks) in your Slack workspace.
2. Export the URL and enable Slack in the BFF:

```bash
export SLACK_WEBHOOK_URL=https://hooks.slack.com/services/...
cd cursr_backend && mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dapp.slack.enabled=true"
```

Dedupe: one Slack message per `(tenant, service, environment)` every 5 minutes (configurable via `app.slack.dedupe-window-minutes`).

**Smoke test:**

```bash
# Manual anomaly (requires dev profile on processor)
curl -X POST 'http://localhost:8080/api/dev/emit-anomaly?serviceName=payment-service&environment=staging'

# Dashboard anomalies slice
curl -s http://localhost:8090/api/dashboard/summary | jq '.anomalies | length'

# ClickHouse
curl 'http://localhost:8123/?query=SELECT service_name,anomaly_score FROM observability.anomalies ORDER BY created_at DESC LIMIT 5'
```

### End-to-end smoke test

```bash
# Wait a few seconds for logs to flow, then:
curl http://localhost:8080/api/logs/recent
curl http://localhost:8090/api/health
```

## Configuration

Spring Boot apps default to `localhost` endpoints matching the Docker stack:

- Kafka: `localhost:29092`
- Redis: `localhost:6379`
- ClickHouse: `localhost:8123/observability`
- OTel Collector: `localhost:4317` (gRPC), `localhost:4318` (HTTP)

See [`.env.example`](.env.example) for optional Docker port overrides.

## Data flow

```
log-sender-backend  ──OTLP HTTP :4318──►  OTel Collector
                                              │
                                              ▼
                                         Kafka (logs.raw, traces.raw)
                                              │
                                              ▼
                                    log-processor-service
                                              │
                         ┌────────────────────┼────────────────────┐
                         ▼                    ▼                    ▼
                    ClickHouse              Redis            anomaly-alerts
                         │                                         │
                         ▼                                         ▼
                   cursr_backend  ◄── Redis pub/sub ─── cursr_backend
                   (REST queries)      WebSocket
```

## Per-service docs

- [log-processor-service/README.md](log-processor-service/README.md)
- [cursr_backend/README.md](cursr_backend/README.md)
- [log-sender-backend/README.md](log-sender-backend/README.md)

## Stopping infrastructure

```bash
docker compose down
```

To remove ClickHouse data as well (required after `log_clusters` schema changes):

```bash
docker compose down -v
docker compose up -d
```

For an existing ClickHouse volume without wiping all data, drop and recreate the clusters rollup:

```sql
DROP VIEW IF EXISTS observability.log_clusters_mv;
DROP TABLE IF EXISTS observability.log_clusters;
-- then re-run infra/clickhouse/init.sql cluster section or restart clickhouse-init
```
