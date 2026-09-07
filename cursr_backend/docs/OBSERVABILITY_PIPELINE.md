# Observability pipeline (Kafka → ClickHouse → UI)

This doc explains how anomalies + RCA flow through the system and how the dashboard UI (`next-monorepo`) should integrate.

## Key principle

The dashboard UI should talk to **one backend**: `cursr_backend`.

- **REST**: the UI fetches dashboard data from `cursr_backend` HTTP APIs.
- **Realtime**: the UI subscribes to `cursr_backend` STOMP/WebSocket topics.

This keeps auth/tenant routing centralized and avoids exposing internal services directly to browsers.

## Components and responsibilities

### `log-processor-service`

- Ingests OTLP logs from Kafka topic `logs.raw` (protobuf payload).
- Stores raw logs into ClickHouse (`logs_hot` / `logs_cold`).
- Builds 30s Kafka Streams windows and (when eligible) calls the AI service over gRPC.
- If an anomaly window is detected, it:
  - inserts a row into ClickHouse table `anomalies`
  - publishes a Kafka message to topic `anomaly-alerts` (for RCA generation)

### `ai_service` (Python)

- HTTP: clusters log lines (`POST /cluster`) using Drain3.
- gRPC: scores aggregated windows (IsolationForest), returning `(anomaly_score, is_anomaly)`.

### `llm-router-service`

- Consumes Kafka topic `anomaly-alerts`.
- Pulls recent ERROR logs from ClickHouse `logs_hot` to build context.
- Calls Anthropic (Claude) to generate a short RCA text.
- Updates ClickHouse: `anomalies.rca_text`.
- (It has its own WebSocket endpoint, but the recommended approach is: UI does **not** connect to it directly.)

### `cursr_backend` (BFF / dashboard backend)

`cursr_backend` is the integration point for the UI.

**Realtime anomalies (already implemented)**:

1. `cursr_backend` consumes anomaly events from Kafka via `AnomalyKafkaListener`.
2. It publishes the raw anomaly event JSON to Redis pub/sub channel:
   - `${app.anomalies.redis-channel-prefix}:{tenantId}`
3. `AnomalyRedisWebSocketBridge` fans out Redis messages to STOMP topic:
   - `/topic/anomalies/{tenantId}`
4. `next-monorepo` subscribes to that topic via WebSocket:
   - broker endpoint: `/ws/anomalies`

**RCA display**:

- RCA is persisted in ClickHouse (`anomalies.rca_text`) by `llm-router-service`.
- The UI can fetch it through `cursr_backend` REST endpoints that read ClickHouse.
- If you want realtime RCA updates, the recommended pattern is:
  - publish an explicit “RCA updated” event to Kafka/Redis and fan it out via `cursr_backend` WebSocket (same model as anomalies).

## Latency notes

- Using `cursr_backend` does **not** add new latency for realtime anomalies in the current setup, because the UI already connects to `cursr_backend` (`/ws/anomalies`) instead of connecting to internal services.
- For RCA:
  - If the UI reads RCA via REST from ClickHouse, the user-visible delay is dominated by the LLM call time (seconds), not by one extra backend hop.
  - If you need realtime RCA delivery, emit an “RCA ready” event and push via WebSocket from `cursr_backend`.

