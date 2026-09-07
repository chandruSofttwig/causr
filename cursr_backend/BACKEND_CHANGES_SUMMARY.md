# Backend changes summary (`cursr_backend/src`)

This file summarizes what was implemented in the backend to support the **dashboard summary snapshot**, **Redis cache**, and **WebSocket-first updates**.

## High-level flow

- **Every 10 seconds**: ClickHouse → `DashboardCacheRefreshJob` → Redis keys (including `dashboard:summary`)
- **Immediately after summary write**: backend publishes the full snapshot to **STOMP** topic `/topic/dashboard/summary`
- **Frontend**:
  - does a **one-shot REST hydration** via `GET /api/dashboard/summary` (ETag enabled)
  - then stays updated via WebSocket frames

## Dashboard Redis cache keys

- **File**: `cursr_backend/src/main/java/com/cursr/backend/dashboard/DashboardCacheKeys.java`
- **Keys**:
  - `dashboard:errorRate`
  - `dashboard:latency`
  - `dashboard:topErrors`
  - `dashboard:risk`
  - `dashboard:errorClusters`
  - `dashboard:summary` (**new aggregated snapshot key**)

## Summary snapshot builder

- **File**: `cursr_backend/src/main/java/com/cursr/backend/dashboard/DashboardSummaryService.java`
- **Responsibility**: builds a single snapshot map:
  - `generatedAt`
  - `errorRate`
  - `latency`
  - `topErrors`
  - `risk`
  - `errorClusters`
- **Data source**: `DashboardQueryService` (reads the cached datasets from Redis and returns safe defaults)

## Cache refresh job (10s) + publish-on-write WS

- **File**: `cursr_backend/src/main/java/com/cursr/backend/dashboard/DashboardCacheRefreshJob.java`
- **Schedule**: `@Scheduled(fixedRate = 10_000)`
- **Behavior**:
  - refreshes each dataset key from ClickHouse SQL
  - builds the aggregated snapshot (`DashboardSummaryService`)
  - writes snapshot to Redis (`DashboardCacheKeys.SUMMARY`)
  - **publishes snapshot immediately** via `SimpMessagingTemplate.convertAndSend("/topic/dashboard/summary", summary)`

This removes the need for a separate “poll Redis every 5s and publish” scheduler.

## Dashboard REST endpoints + ETag hydration

- **File**: `cursr_backend/src/main/java/com/cursr/backend/dashboard/DashboardController.java`
- **Existing endpoints**:
  - `GET /api/dashboard/error-rate`
  - `GET /api/dashboard/p95-latency`
  - `GET /api/dashboard/top-errors`
  - `GET /api/dashboard/failure-risk`
  - `GET /api/dashboard/error-clusters`
  - `GET /api/dashboard/trace/{traceId}`
- **Added**:
  - `GET /api/dashboard/summary`
    - returns the full snapshot
    - adds `ETag`
    - supports `If-None-Match` → returns `304 Not Modified` when unchanged
    - reads from Redis `dashboard:summary` first, falls back to rebuilding if missing

## WebSocket/STOMP configuration + guardrails

- **File**: `cursr_backend/src/main/java/com/cursr/backend/config/WebSocketBrokerConfig.java`
- **STOMP endpoint**: `/ws/anomalies`
- **Broker prefixes**:
  - broker: `/topic`
  - app prefix: `/app`
- **Transport limits**:
  - message size limit: 64 KB
  - send buffer: 512 KB
  - send time limit: 20 s
- **Connection cap**:
  - handshake interceptor `WebSocketConnectionLimitInterceptor`
  - configurable via `app.websocket.max-connections`

### Connection cap implementation

- **File**: `cursr_backend/src/main/java/com/cursr/backend/config/WebSocketConnectionLimitInterceptor.java`
- **Behavior**:
  - counts active WS connections
  - rejects new handshakes once `maxConnections` is exceeded
  - decrements on `SessionDisconnectEvent`

## CORS for local dashboard development

- **File**: `cursr_backend/src/main/java/com/cursr/backend/config/CorsConfig.java`
- **Allows**: `http://localhost:3000` (configurable)
- **Scope**: `/api/dashboard/**`

## Configuration knobs

- **File**: `cursr_backend/src/main/resources/application.yml`
- **Added**:
  - `app.cors.allowed-origin` (default `http://localhost:3000`)
  - `app.websocket.allowed-origin` (default `http://localhost:3000`)
  - `app.websocket.max-connections` (default `300`)

