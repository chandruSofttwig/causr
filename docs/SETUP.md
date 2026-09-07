# Causr setup guide

Step-by-step local setup for the Causr observability stack.

For architecture details see [PROJECT.md](../PROJECT.md). For instrumenting real services see [INSTRUMENTATION.md](INSTRUMENTATION.md). For ideas to shrink this further see [SIMPLIFY_SETUP_PLAN.md](SIMPLIFY_SETUP_PLAN.md).

---

## Prerequisites

| Tool | Version | Why |
|------|---------|-----|
| Docker + Compose | recent | Infra (Kafka, Redis, ClickHouse, OTel, AI) |
| Java (JDK) | 17+ (21 preferred for BFF / llm-router / sender) | Spring services |
| Maven | 3.9+ | Build/run Spring apps |
| Node.js | 20+ (22 OK) | `causr-dashboard` |
| Optional | `GROQ_API_KEY` | RCA text via Groq (`llm-router-service`) |
| Optional | Slack Incoming Webhook | Anomaly alerts |

---

## Path A — One command (recommended)

Runs infrastructure **and** app containers (processor, BFF, sender, dashboard, llm-router).

```bash
cd /path/to/causr
cp .env.example .env   # edit APP_SECURITY_API_KEY / GROQ_API_KEY if needed

docker compose --profile apps up -d --build
```

| Service | URL |
|---------|-----|
| Dashboard | http://localhost:5173 |
| BFF | http://localhost:8090 |
| Log processor | http://localhost:8080 |
| LLM router | http://localhost:8091 |

Default API key: `causr-local-key` (header `X-API-Key`). Dashboard env is baked at image build time from `APP_SECURITY_API_KEY`.

### Smoke test

```bash
curl -s -H "X-API-Key: causr-local-key" -H "X-Tenant-Id: default" \
  http://localhost:8090/api/dashboard/summary | head -c 200

curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8123/ping
```

Emit a test anomaly (processor must be on `dev` profile — compose sets this):

```bash
curl -X POST -H "X-API-Key: causr-local-key" \
  'http://localhost:8080/api/dev/emit-anomaly?serviceName=payment-service&environment=staging'
```

Stop:

```bash
docker compose --profile apps down
# wipe ClickHouse volume after schema changes:
# docker compose --profile apps down -v
```

---

## Path B — Infra in Docker, apps on the host (dev loop)

### 1. Infra only

```bash
cp .env.example .env
docker compose up -d
```

Wait until Kafka, Redis, ClickHouse, OTel Collector, and `ai-service` are healthy.

### 2. Applications (separate terminals)

```bash
# Terminal 1 — ingest + anomaly pipeline
cd log-processor-service
SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run

# Terminal 2 — synthetic OTLP traffic
cd log-sender-backend
mvn spring-boot:run

# Terminal 3 — dashboard BFF
cd cursr_backend
mvn spring-boot:run
# Slack (optional):
# export SLACK_WEBHOOK_URL=https://hooks.slack.com/services/...
# mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dapp.slack.enabled=true"

# Terminal 4 — RCA (optional)
export GROQ_API_KEY=gsk_...
# optional: export GROQ_MODEL=qwen/qwen3.6-27b
cd llm-router-service
mvn spring-boot:run

# Terminal 5 — UI
cd causr-dashboard
cp .env.example .env   # VITE_API_KEY must match APP_SECURITY_API_KEY
npm install
npm run dev
```

Open http://localhost:5173.

Host apps talk to:

- Kafka `localhost:29092`
- Redis `localhost:6379`
- ClickHouse `localhost:8123`
- OTel HTTP `localhost:4318`
- AI gRPC `localhost:50051`

---

## Configuration cheatsheet

| Variable | Default | Used by |
|----------|---------|---------|
| `APP_SECURITY_API_KEY` | `causr-local-key` | BFF, processor, dashboard |
| `VITE_API_KEY` | same | Dashboard browser calls |
| `VITE_TENANT_ID` | `default` | Dashboard tenant header + WS topic |
| `GROQ_API_KEY` | empty | llm-router Groq RCA (skipped if unset) |
| `GROQ_MODEL` | `qwen/qwen3.6-27b` | Groq model id for RCA |
| `GROQ_BASE_URL` | `https://api.groq.com/openai/v1` | Groq API base |
| `SLACK_WEBHOOK_URL` | empty | BFF Slack alerts |

---

## Common failures

| Symptom | Fix |
|---------|-----|
| Dashboard 401 | Match `VITE_API_KEY` and `APP_SECURITY_API_KEY` |
| No logs in UI | Ensure sender + collector + processor; check `docker compose ps` |
| Empty anomalies | Processor `dev` profile + `ai-service` up; or use `emit-anomaly` |
| No RCA | Set `GROQ_API_KEY` and run `llm-router-service` |
| Port in use | `ss -tlnp \| grep -E ':8080\|:8090\|:5173'` and free the port |

---

## What you get when it works

1. Synthetic microservices emit OTLP logs → Kafka → ClickHouse  
2. Dashboard shows KPIs, logs, anomalies (live WebSocket when BFF is up)  
3. Optional Slack on anomaly; optional Groq RCA in the anomaly detail panel  
4. Trace links from logs when `trace_id` is present  
