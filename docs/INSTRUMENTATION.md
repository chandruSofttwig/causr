# Instrumenting services for Causr

Causr ingests **OpenTelemetry (OTLP)** logs (and traces) via the collector → Kafka → `log-processor-service` → ClickHouse.

## Endpoint

Point your OTLP exporter at the collector:

| Protocol | Default URL |
|----------|-------------|
| OTLP HTTP | `http://localhost:4318` |
| OTLP gRPC | `http://localhost:4317` |

## Required / recommended attributes

| Attribute | Where | Purpose |
|-----------|--------|---------|
| `service.name` | resource **and/or** log/span | Service identity (log-record overrides resource) |
| `deployment.environment` | resource or log | Environment (`prod`, `staging`, …) |
| `tenant.id` | resource or log | Multi-tenant isolation (`default` if omitted) |
| `app.latency_ms` or duration attrs | log | Populates `duration_ms` / P99 KPIs |
| `http.status_code` | log/span | Error-rate numerator for 5xx |
| `issue.type` | log | Stable clustering key |

## Java agent example

```bash
java -javaagent:/path/to/opentelemetry-javaagent.jar \
  -Dotel.service.name=my-service \
  -Dotel.resource.attributes=deployment.environment=staging,tenant.id=default \
  -Dotel.exporter.otlp.endpoint=http://localhost:4318 \
  -Dotel.exporter.otlp.protocol=http/protobuf \
  -jar my-service.jar
```

## API access (dashboard BFF)

Dashboard and processor HTTP APIs expect:

- Header `X-API-Key: <APP_SECURITY_API_KEY>` (default local: `causr-local-key`)
- Header `X-Tenant-Id: default` (or your tenant)

WebSocket STOMP: `ws://localhost:8090/ws/anomalies?apiKey=...&tenantId=default`

## Synthetic traffic

For local demos without your own apps, run `log-sender-backend` (emits a microservices mesh including `tenant.id=default`).
