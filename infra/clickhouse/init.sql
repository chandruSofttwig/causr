CREATE DATABASE IF NOT EXISTS observability;

CREATE TABLE IF NOT EXISTS observability.logs_hot
(
    timestamp DateTime,
    tenant_id String,
    service_name String,
    log_level String,
    message String,
    trace_id String,
    span_id String,
    duration_ms UInt32,
    anomaly_score Float32,
    cluster_id String,
    attributes Map(String, String)
)
ENGINE = MergeTree
PARTITION BY toDate(timestamp)
ORDER BY (tenant_id, service_name, timestamp, trace_id)
TTL toDate(timestamp) + INTERVAL 30 DAY;

CREATE TABLE IF NOT EXISTS observability.logs_cold
(
    timestamp DateTime,
    tenant_id String,
    service_name String,
    log_level String,
    message String,
    trace_id String,
    span_id String,
    duration_ms UInt32,
    anomaly_score Float32,
    cluster_id String,
    attributes Map(String, String)
)
ENGINE = MergeTree
PARTITION BY toDate(timestamp)
ORDER BY (tenant_id, service_name, timestamp, trace_id)
TTL toDate(timestamp) + INTERVAL 90 DAY;

CREATE TABLE IF NOT EXISTS observability.spans
(
    start_time DateTime64(3),
    trace_id String,
    span_id String,
    parent_span_id String,
    tenant_id String,
    service_name String,
    environment String,
    span_name String,
    span_kind String,
    status_code String,
    duration_ms UInt32,
    attributes Map(String, String)
)
ENGINE = MergeTree
PARTITION BY toDate(start_time)
ORDER BY (tenant_id, service_name, start_time, trace_id, span_id)
TTL toDate(start_time) + INTERVAL 30 DAY;

CREATE TABLE IF NOT EXISTS observability.log_clusters
(
    tenant_id String,
    service_name String,
    cluster_id String,
    representative_message String,
    event_count UInt64
)
ENGINE = SummingMergeTree((event_count))
PARTITION BY tuple()
ORDER BY (tenant_id, service_name, cluster_id, representative_message);

CREATE MATERIALIZED VIEW IF NOT EXISTS observability.log_clusters_mv TO observability.log_clusters
AS
SELECT
    tenant_id,
    service_name,
    cluster_key AS cluster_id,
    any(message) AS representative_message,
    toUInt64(count()) AS event_count
FROM
(
    SELECT
        ifNull(tenant_id, '') AS tenant_id,
        ifNull(service_name, '') AS service_name,
        if(
            cluster_id != '',
            cluster_id,
            if(
                attributes['issue.type'] != '',
                attributes['issue.type'],
                toString(cityHash64(ifNull(message, '')))
            )
        ) AS cluster_key,
        message
    FROM observability.logs_hot
)
GROUP BY tenant_id, service_name, cluster_key;

CREATE TABLE IF NOT EXISTS observability.service_metrics
(
    window_start DateTime,
    window_end DateTime,
    tenant_id String,
    service_name String,
    environment String,
    log_volume UInt64,
    error_volume UInt64,
    error_rate Float32,
    p99_latency_ms Float32,
    unique_error_types UInt32,
    new_error_types UInt32,
    silence_flag UInt8,
    deployment_flag UInt8,
    time_of_day_sin Float32,
    time_of_day_cos Float32,
    ai_anomaly_score Float32,
    created_at DateTime DEFAULT now()
)
ENGINE = MergeTree
PARTITION BY toDate(window_start)
ORDER BY (tenant_id, service_name, environment, window_start)
TTL toDateTime(window_start) + INTERVAL 180 DAY;

CREATE TABLE IF NOT EXISTS observability.anomalies
(
    id UUID DEFAULT generateUUIDv4(),
    window_start DateTime,
    window_end DateTime,
    tenant_id String,
    service_name String,
    environment String,
    anomaly_score Float32,
    is_anomaly UInt8,
    feature_json String,
    rca_text String,
    rca_generated_at Nullable(DateTime),
    created_at DateTime DEFAULT now()
)
ENGINE = MergeTree
PARTITION BY toDate(window_start)
ORDER BY (service_name, environment, window_start, id)
TTL toDateTime(window_start) + INTERVAL 90 DAY;
