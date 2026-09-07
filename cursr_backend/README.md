# cursr-backend

Spring Boot backend for Cursr.

Includes optional OpenTelemetry log testing (`RandomLogGeneratorService`) and **live dashboard REST APIs** that query ClickHouse `observability.logs_hot`.

## Requirements

- Java 21+
- Maven 3.9+
- ClickHouse at `192.168.1.35:8123` (configurable) with `observability.logs_hot` populated by your ingest pipeline

## Run

```bash
mvn spring-boot:run
```

### ClickHouse

Configure in `src/main/resources/application.yml`:

- `clickhouse.url` (default `jdbc:clickhouse://192.168.1.35:8123/observability`)
- `clickhouse.username`
- `clickhouse.password`

### OpenTelemetry log generator (optional)

- `app.log-generator.enabled`
- `app.log-generator.initial-delay-ms`
- `app.log-generator.fixed-delay-ms`
- `otel.exporter.otlp.endpoint`

### Health

- `http://localhost:8080/api/health`
- `http://localhost:8080/actuator/health`

## Live dashboard API

All responses are JSON arrays of row objects (empty when no data).

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/dashboard/error-rate` | Error rate per service (last 5 minutes) |
| GET | `/api/dashboard/p95-latency` | P95 `duration_ms` per service (last 5 minutes) |
| GET | `/api/dashboard/top-errors` | Top error messages (last 1 hour, limit 10) |
| GET | `/api/dashboard/trace/{traceId}` | Logs for a trace (bound parameter, safe for SQL) |
| GET | `/api/dashboard/failure-risk` | Pre-failure risk score (last 10 minutes, `risk_score > 40`) |
| GET | `/api/dashboard/error-clusters` | Top rows from `observability.log_clusters` (requires MV + table) |

**Risk score (heuristic):** roughly 0–40 normal, 40–70 elevated, 70–100 critical (see SQL in `DashboardQueryService`).

## ClickHouse schema

See [docs/clickhouse/logs_schema.md](docs/clickhouse/logs_schema.md) for `logs_hot` / `logs_cold` column layout.

Materialized view DDL for error clustering (apply on ClickHouse): [../docs/clickhouse/log_clusters_mv.sql](../docs/clickhouse/log_clusters_mv.sql).

**Ingest:** if writers still target `observability.logs` only, ensure data reaches `logs_hot` (ETL, MV, or dual write).

## Test

```bash
mvn test
```

`src/test/resources/application.yml` duplicates `clickhouse.*` so the full context loads in CI without relying on config merge quirks.

### SQL-level dashboard tests (Testcontainers)

[`DashboardClickHouseSqlIntegrationTest`](src/test/java/com/cursr/backend/dashboard/DashboardClickHouseSqlIntegrationTest.java) runs every query in [`DashboardSql`](src/main/java/com/cursr/backend/dashboard/DashboardSql.java) against a real ClickHouse 24.3 instance in Docker. It is annotated with `@Testcontainers(disabledWithoutDocker = true)`, so **without Docker the class is skipped** and `mvn test` still passes.

To execute these tests locally or in CI:

- Use a working Docker engine compatible with Testcontainers (Docker API 1.44+).
- Run `mvn test`; the ClickHouse image `clickhouse/clickhouse-server:24.3.6.48` is pulled on first run.

[`DashboardControllerTest`](src/test/java/com/cursr/backend/dashboard/DashboardControllerTest.java) adds MockMvc coverage for `/api/dashboard/summary`, `service-metrics-recent`, and anomaly detail/logs without ClickHouse.
