## `ai_service` — Log Clustering (HTTP) + Anomaly Scoring (gRPC)

This document describes the Python AI service that supports the observability pipeline.

### What this service is

- **Language/runtime**: Python
- **HTTP API**: FastAPI
- **gRPC API**: protobuf-defined `AnomalyScorer` service
- **Role**:
  - cluster individual log lines into templates (Drain3)
  - score aggregated service-metric windows for anomaly detection (IsolationForest)

### Interfaces

#### HTTP: clustering

- `POST /cluster`
- Request body:
  - `log_line: string`
- Response:
  - `cluster_id`: stable identifier for the template
  - `template`: mined template string

Used by:

- `log-processor-service` (during ingestion) when clustering is enabled

#### gRPC: anomaly scoring

Service:

- `AnomalyScorer.ScoreBatch(ScoreBatchRequest) -> ScoreBatchResponse`

Key message:

- `FeatureVector`:
  - tenant/service/environment/window timestamps
  - window features such as `error_rate`, `log_volume`, latency, flags, etc.

Response:

- `anomaly_score: float`
- `is_anomaly: bool`

Used by:

- `log-processor-service` window handler (`ServiceMetricsWindowHandler`)

### Model behavior

On startup, the service attempts to train from ClickHouse:

- reads recent rows from `service_metrics`
- if insufficient rows exist (e.g., < 20), it falls back to a synthetic baseline fit

The anomaly model:

- scales features using `RobustScaler`
- scores with `IsolationForest`
- uses a fixed threshold (example in current code): mark anomaly when `score < -0.4`

### Dependencies

- ClickHouse (for training data)
- No Kafka/Redis dependency in this service itself

### Local run

Common container ports:

- HTTP: `8000`
- gRPC: `50051`

Typical Docker usage (from `log-processor-service/ai_service`):

```bash
docker build -t log-processor-ai .
docker run -p 8000:8000 -p 50051:50051 log-processor-ai
```

### Environment variables

The service reads ClickHouse connection info via env:

- `CH_HOST` (default `localhost`)
- `CH_PORT` (default `8123`)
- `CH_DATABASE` (default `observability`)
- `CH_USER` (default `default`)
- `CH_PASSWORD` (default empty)

### Where to look in code

- FastAPI + gRPC server:
  - `log-processor-service/ai_service/app.py`
- Proto contract (shared with Java gRPC client):
  - `log-processor-service/src/main/proto/anomaly_scorer.proto`

