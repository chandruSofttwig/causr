# Log Processor Service

Spring Boot service that consumes **OTLP Logs protobuf** from Kafka, applies **sampling**, and writes rows to **ClickHouse** (`logs_hot` / `logs_cold`). It also exposes metrics via Spring Actuator and supports anomaly scoring + clustering through the bundled AI service.

## What you need

- Java 17+ and Maven
- Running services:
  - Kafka + Zookeeper
  - ClickHouse (with the `observability` schema)
  - Redis (on `localhost:6379`)
  - Optional: AI service (gRPC `50051` + HTTP `8000`) for anomaly scoring/clustering

## Endpoints

- Health: `GET /actuator/health`
- Prometheus metrics: `GET /actuator/prometheus`
- Recent logs: `GET /api/logs/recent`
- Recent search: `GET /api/search?q=...`
- Clusters: `GET /api/clusters`
- Anomalies: `GET /api/anomalies`

## Run (Docker-based local setup)

### 1) Start Kafka/Zookeeper

From `log-processor-service/`:

```bash
docker compose up -d
```

The provided `docker-compose.yml` starts only `zookeeper` + `kafka`.

### 2) Start ClickHouse + Redis

Example (adjust versions/ports if needed):

```bash
docker run -d --name clickhouse \
  -p 8123:8123 -p 9000:9000 \
  --ulimit nofile=262144:262144 \
  clickhouse/clickhouse-server:24.3

docker run -d --name redis \
  -p 6379:6379 \
  redis:7
```

### 3) Create ClickHouse schema

```bash
docker exec -i clickhouse clickhouse-client -q "CREATE DATABASE IF NOT EXISTS observability;"
docker exec -i clickhouse clickhouse-client --multi -n observability < "src/main/resources/db/clickhouse/schema.sql"
```

### 4) Create Kafka topic `logs.raw`

The app expects **binary OTLP `LogsData`** messages on the `logs.raw` topic (not JSON strings).

```bash
docker exec kafka kafka-topics --bootstrap-server kafka:9092 \
  --create --topic logs.raw --partitions 1 --replication-factor 1 --if-not-exists
```

### 5) (Optional) Start the AI service

From `log-processor-service/`:

```bash
docker build -t log-processor-ai ./ai_service
docker run -d --name log-processor-ai \
  -p 8000:8000 -p 50051:50051 \
  --link clickhouse:clickhouse \
  log-processor-ai
```

## Configure the Spring Boot app

Edit `src/main/resources/application.yml` if your environment differs from defaults:

- `spring.kafka.bootstrap-servers` (default expects `192.168.1.77:29092`)
- `clickhouse.url` (default `jdbc:clickhouse://localhost:8123/observability`)
- `app.ai.grpc-target` / `app.ai.http-base-url` (default `localhost:50051` / `http://localhost:8000`)
- Redis is configured via Spring Boot defaults for `localhost:6379` (see `spring.data.redis` if you change it)

## Start the service

Option A (recommended):

```bash
mvn -f pom.xml -DskipTests package
java -jar target/log-processor-service-0.0.1-SNAPSHOT.jar
```

Option B:

```bash
mvn -f pom.xml -DskipTests spring-boot:run
```

## Verify

```bash
curl -s http://localhost:8080/actuator/health
curl -s http://localhost:8080/actuator/prometheus
```

