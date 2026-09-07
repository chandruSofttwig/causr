"""AI service: gRPC IsolationForest scoring + HTTP Drain3 /cluster."""

from __future__ import annotations

import os
import sys
import threading
from concurrent import futures

sys.path.insert(0, os.path.join(os.path.dirname(__file__), "generated"))

import anomaly_scorer_pb2
import anomaly_scorer_pb2_grpc
import clickhouse_connect
import grpc
import numpy as np
import uvicorn
from drain3 import TemplateMiner
from fastapi import FastAPI
from pydantic import BaseModel
from sklearn.ensemble import IsolationForest
from sklearn.preprocessing import RobustScaler

FEATURE_COLS = [
    "error_rate",
    "log_volume",
    "p99_latency_ms",
    "unique_error_types",
    "new_error_types",
    "silence_flag",
    "deployment_flag",
    "time_of_day_sin",
    "time_of_day_cos",
]


class ModelState:
    def __init__(self) -> None:
        self.scaler: RobustScaler | None = None
        self.model: IsolationForest | None = None


state = ModelState()
miner = TemplateMiner()
app = FastAPI(title="Observability AI")


def _fallback_fit() -> None:
    state.scaler = RobustScaler()
    rng = np.random.default_rng(0)
    x = rng.normal(size=(200, len(FEATURE_COLS)))
    xs = state.scaler.fit_transform(x)
    state.model = IsolationForest(n_estimators=100, contamination=0.01, random_state=42)
    state.model.fit(xs)


def train() -> None:
    host = os.environ.get("CH_HOST", "localhost")
    try:
        client = clickhouse_connect.get_client(
            host=host,
            port=int(os.environ.get("CH_PORT", "8123")),
            database=os.environ.get("CH_DATABASE", "observability"),
            username=os.environ.get("CH_USER", "default"),
            password=os.environ.get("CH_PASSWORD", ""),
        )
        result = client.query(
            """
            SELECT error_rate, log_volume, p99_latency_ms,
                   unique_error_types, new_error_types, silence_flag, deployment_flag,
                   time_of_day_sin, time_of_day_cos
            FROM service_metrics
            WHERE window_start >= now() - INTERVAL 7 DAY
            """
        )
        rows = result.result_rows
        if rows is None or len(rows) < 20:
            _fallback_fit()
            return
        x = np.array(rows, dtype=float)
        state.scaler = RobustScaler()
        xs = state.scaler.fit_transform(x)
        state.model = IsolationForest(n_estimators=100, contamination=0.01, random_state=42)
        state.model.fit(xs)
    except Exception:
        _fallback_fit()


@app.on_event("startup")
async def on_startup() -> None:
    train()


class ClusterBody(BaseModel):
    log_line: str


@app.post("/cluster")
def cluster_endpoint(body: ClusterBody):
    res = miner.add_log_message(body.log_line or "")
    return {
        "cluster_id": res.cluster.cluster_id,
        "template": res.cluster.get_template(),
    }


class Servicer(anomaly_scorer_pb2_grpc.AnomalyScorerServicer):
    def ScoreBatch(self, request, context):
        if state.model is None or state.scaler is None:
            train()
        resp = anomaly_scorer_pb2.ScoreBatchResponse()
        for v in request.vectors:
            row = np.array(
                [
                    [
                        v.error_rate,
                        v.log_volume,
                        v.p99_latency_ms,
                        v.unique_error_types,
                        v.new_error_types,
                        v.silence_flag,
                        v.deployment_flag,
                        v.time_of_day_sin,
                        v.time_of_day_cos,
                    ]
                ],
                dtype=float,
            )
            x = state.scaler.transform(row)
            s = float(state.model.score_samples(x)[0])
            r = resp.results.add()
            r.anomaly_score = s
            r.is_anomaly = s < -0.4
        return resp


def serve_grpc() -> None:
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=8))
    anomaly_scorer_pb2_grpc.add_AnomalyScorerServicer_to_server(Servicer(), server)
    port = os.environ.get("GRPC_PORT", "50051")
    server.add_insecure_port(f"[::]:{port}")
    server.start()
    server.wait_for_termination()


if __name__ == "__main__":
    threading.Thread(target=serve_grpc, daemon=True).start()
    uvicorn.run(app, host="0.0.0.0", port=int(os.environ.get("HTTP_PORT", "8000")))
