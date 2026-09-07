package com.cursr.backend.dashboard;

/**
 * ClickHouse SQL shared by {@link DashboardCacheRefreshJob} (write path) and
 * {@link DashboardQueryService} (trace lookup).
 */
public final class DashboardSql {

  private DashboardSql() {
  }

  /**
   * Per-service error rate as percentage (0–100). Numerator: ERROR logs or HTTP 5xx in {@code attributes}.
   * Denominator: all log lines in window (proxy for request volume when one row ≈ one observation).
   */
  public static final String ERROR_RATE_LAST_5_MIN = """
    SELECT
        service_name,
        countIf(
            log_level = 'ERROR'
            OR toUInt16OrZero(attributes['http.status_code']) >= 500
        ) AS errors,
        count() AS total,
        round(errors * 100.0 / nullIf(total, 0), 3) AS error_rate
    FROM observability.logs_hot
    WHERE timestamp >= now() - INTERVAL 5 MINUTE
      AND if(tenant_id = '', 'default', tenant_id) = ?
    GROUP BY service_name
    ORDER BY error_rate DESC
    """;

  /** Global error rate % for current and previous 5-minute windows (for KPI compare). */
  public static final String ERROR_RATE_GLOBAL_TWO_WINDOWS = """
    SELECT
        'current' AS window_label,
        countIf(
            log_level = 'ERROR'
            OR toUInt16OrZero(attributes['http.status_code']) >= 500
        ) * 100.0 / nullIf(count(), 0) AS error_rate_pct,
        count() AS total
    FROM observability.logs_hot
    WHERE timestamp >= now() - INTERVAL 5 MINUTE
      AND if(tenant_id = '', 'default', tenant_id) = ?
    UNION ALL
    SELECT
        'previous',
        countIf(
            log_level = 'ERROR'
            OR toUInt16OrZero(attributes['http.status_code']) >= 500
        ) * 100.0 / nullIf(count(), 0),
        count()
    FROM observability.logs_hot
    WHERE timestamp >= now() - INTERVAL 10 MINUTE
      AND timestamp < now() - INTERVAL 5 MINUTE
      AND if(tenant_id = '', 'default', tenant_id) = ?
    """;

  /** Global p99 latency (ms) for current and previous 5-minute windows. */
  public static final String P99_GLOBAL_TWO_WINDOWS = """
    SELECT
        'current' AS window_label,
        quantile(0.99)(duration_ms) AS p99_ms
    FROM observability.logs_hot
    WHERE timestamp >= now() - INTERVAL 5 MINUTE
      AND if(tenant_id = '', 'default', tenant_id) = ?
    UNION ALL
    SELECT
        'previous',
        quantile(0.99)(duration_ms)
    FROM observability.logs_hot
    WHERE timestamp >= now() - INTERVAL 10 MINUTE
      AND timestamp < now() - INTERVAL 5 MINUTE
      AND if(tenant_id = '', 'default', tenant_id) = ?
    """;

  /** Requests/min (log lines / 60) for current and previous 1-minute windows. */
  public static final String RPM_GLOBAL_TWO_WINDOWS = """
    SELECT
        'current' AS window_label,
        count() / 60.0 AS rpm
    FROM observability.logs_hot
    WHERE timestamp >= now() - INTERVAL 1 MINUTE
      AND if(tenant_id = '', 'default', tenant_id) = ?
    UNION ALL
    SELECT
        'previous',
        count() / 60.0
    FROM observability.logs_hot
    WHERE timestamp >= now() - INTERVAL 2 MINUTE
      AND timestamp < now() - INTERVAL 1 MINUTE
      AND if(tenant_id = '', 'default', tenant_id) = ?
    """;

  public static final String P95_LATENCY_LAST_5_MIN = """
    SELECT
        service_name,
        quantile(0.95)(duration_ms) AS p95_latency,
        quantile(0.99)(duration_ms) AS p99_latency
    FROM observability.logs_hot
    WHERE timestamp >= now() - INTERVAL 5 MINUTE
      AND if(tenant_id = '', 'default', tenant_id) = ?
    GROUP BY service_name
    ORDER BY p95_latency DESC
    """;

  /** Last 30 minutes vs prior 30 minutes for trend (↑/↓/flat on the UI). */
  public static final String TOP_ERRORS_30M_WITH_TREND = """
    SELECT
        c.service_name AS service_name,
        c.message AS message,
        toUInt64(c.cnt) AS error_count,
        toUInt64(coalesce(p.cnt, 0)) AS previous_count
    FROM
    (
        SELECT service_name, message, count() AS cnt
        FROM observability.logs_hot
        WHERE (
            log_level = 'ERROR'
            OR toUInt16OrZero(attributes['http.status_code']) >= 500
        )
          AND timestamp >= now() - INTERVAL 30 MINUTE
          AND if(tenant_id = '', 'default', tenant_id) = ?
        GROUP BY service_name, message
    ) AS c
    LEFT JOIN
    (
        SELECT service_name, message, count() AS cnt
        FROM observability.logs_hot
        WHERE (
            log_level = 'ERROR'
            OR toUInt16OrZero(attributes['http.status_code']) >= 500
        )
          AND timestamp >= now() - INTERVAL 60 MINUTE
          AND timestamp < now() - INTERVAL 30 MINUTE
          AND if(tenant_id = '', 'default', tenant_id) = ?
        GROUP BY service_name, message
    ) AS p ON c.service_name = p.service_name AND c.message = p.message
    ORDER BY error_count DESC
    LIMIT 10
    """;

  /**
   * When &gt; 0 in the last 10 minutes, {@link DashboardCacheRefreshJob} prefers span-based latency /
   * error / RPM / service health over {@code logs_hot}.
   */
  public static final String SPANS_SERVER_RECENT_COUNT = """
    SELECT count()
    FROM observability.spans
    WHERE start_time >= now() - INTERVAL 10 MINUTE
      AND span_kind = 'SERVER'
      AND if(tenant_id = '', 'default', tenant_id) = ?
    """;

  /** Per-service error rate from SERVER spans (status ERROR or HTTP 5xx on attributes). */
  public static final String ERROR_RATE_LAST_5_MIN_SPANS = """
    SELECT
        service_name,
        countIf(
            status_code = 'ERROR'
            OR toUInt16OrZero(attributes['http.status_code']) >= 500
            OR toUInt16OrZero(attributes['http.response.status_code']) >= 500
        ) AS errors,
        count() AS total,
        round(errors * 100.0 / nullIf(total, 0), 3) AS error_rate
    FROM observability.spans
    WHERE start_time >= now() - INTERVAL 5 MINUTE
      AND span_kind = 'SERVER'
      AND if(tenant_id = '', 'default', tenant_id) = ?
    GROUP BY service_name
    ORDER BY error_rate DESC
    """;

  public static final String ERROR_RATE_GLOBAL_TWO_WINDOWS_SPANS = """
    SELECT
        'current' AS window_label,
        countIf(
            status_code = 'ERROR'
            OR toUInt16OrZero(attributes['http.status_code']) >= 500
            OR toUInt16OrZero(attributes['http.response.status_code']) >= 500
        ) * 100.0 / nullIf(count(), 0) AS error_rate_pct,
        count() AS total
    FROM observability.spans
    WHERE start_time >= now() - INTERVAL 5 MINUTE
      AND span_kind = 'SERVER'
      AND if(tenant_id = '', 'default', tenant_id) = ?
    UNION ALL
    SELECT
        'previous',
        countIf(
            status_code = 'ERROR'
            OR toUInt16OrZero(attributes['http.status_code']) >= 500
            OR toUInt16OrZero(attributes['http.response.status_code']) >= 500
        ) * 100.0 / nullIf(count(), 0),
        count()
    FROM observability.spans
    WHERE start_time >= now() - INTERVAL 10 MINUTE
      AND start_time < now() - INTERVAL 5 MINUTE
      AND span_kind = 'SERVER'
      AND if(tenant_id = '', 'default', tenant_id) = ?
    """;

  public static final String P99_GLOBAL_TWO_WINDOWS_SPANS = """
    SELECT
        'current' AS window_label,
        quantileIf(0.99)(duration_ms, duration_ms > 0) AS p99_ms
    FROM observability.spans
    WHERE start_time >= now() - INTERVAL 5 MINUTE
      AND span_kind = 'SERVER'
      AND if(tenant_id = '', 'default', tenant_id) = ?
    UNION ALL
    SELECT
        'previous',
        quantileIf(0.99)(duration_ms, duration_ms > 0)
    FROM observability.spans
    WHERE start_time >= now() - INTERVAL 10 MINUTE
      AND start_time < now() - INTERVAL 5 MINUTE
      AND span_kind = 'SERVER'
      AND if(tenant_id = '', 'default', tenant_id) = ?
    """;

  public static final String RPM_GLOBAL_TWO_WINDOWS_SPANS = """
    SELECT
        'current' AS window_label,
        count() / 60.0 AS rpm
    FROM observability.spans
    WHERE start_time >= now() - INTERVAL 1 MINUTE
      AND span_kind = 'SERVER'
      AND if(tenant_id = '', 'default', tenant_id) = ?
    UNION ALL
    SELECT
        'previous',
        count() / 60.0
    FROM observability.spans
    WHERE start_time >= now() - INTERVAL 2 MINUTE
      AND start_time < now() - INTERVAL 1 MINUTE
      AND span_kind = 'SERVER'
      AND if(tenant_id = '', 'default', tenant_id) = ?
    """;

  public static final String P95_LATENCY_LAST_5_MIN_SPANS = """
    SELECT
        service_name,
        quantileIf(0.95)(duration_ms, duration_ms > 0) AS p95_latency,
        quantileIf(0.99)(duration_ms, duration_ms > 0) AS p99_latency
    FROM observability.spans
    WHERE start_time >= now() - INTERVAL 5 MINUTE
      AND span_kind = 'SERVER'
      AND if(tenant_id = '', 'default', tenant_id) = ?
    GROUP BY service_name
    ORDER BY p95_latency DESC
    """;

  /** Per-service health from SERVER spans (5m): error %, p99 latency, RPS. */
  public static final String SERVICE_HEALTH_FROM_SPANS_5M = """
    SELECT
        service_name,
        round(
            countIf(
                status_code = 'ERROR'
                OR toUInt16OrZero(attributes['http.status_code']) >= 500
                OR toUInt16OrZero(attributes['http.response.status_code']) >= 500
            ) * 100.0 / nullIf(count(), 0),
            3
        ) AS error_percent,
        quantileIf(0.99)(duration_ms, duration_ms > 0) AS p99_ms,
        count() / 300.0 AS rps
    FROM observability.spans
    WHERE start_time >= now() - INTERVAL 5 MINUTE
      AND span_kind = 'SERVER'
      AND if(tenant_id = '', 'default', tenant_id) = ?
    GROUP BY service_name
    ORDER BY error_percent DESC
    """;

  public static final String TRACE_LOGS_BY_TRACE_ID = """
    SELECT
        trace_id,
        service_name,
        log_level,
        message,
        timestamp
    FROM observability.logs_hot
    WHERE trace_id = ?
      AND if(tenant_id = '', 'default', tenant_id) = ?
    ORDER BY timestamp
    """;

  public static final String TRACE_SPANS_BY_TRACE_ID = """
    SELECT
        toString(start_time) AS start_time,
        trace_id,
        span_id,
        parent_span_id,
        service_name,
        span_name,
        span_kind,
        status_code,
        duration_ms,
        attributes
    FROM observability.spans
    WHERE trace_id = ?
      AND if(tenant_id = '', 'default', tenant_id) = ?
    ORDER BY start_time ASC
    """;

  public static final String FAILURE_RISK_LAST_10_MIN = """
    SELECT
        service_name,
        countIf(message LIKE '%timeout%') AS timeouts,
        countIf(message LIKE '%connection%') AS connection_errors,
        countIf(log_level = 'WARN') AS warns,
        least(100,
            timeouts * 20 +
            connection_errors * 15 +
            warns * 5
        ) AS risk_score
    FROM observability.logs_hot
    WHERE timestamp >= now() - INTERVAL 10 MINUTE
      AND if(tenant_id = '', 'default', tenant_id) = ?
    GROUP BY service_name
    HAVING risk_score > 40
    ORDER BY risk_score DESC
    """;

  public static final String TOP_ERROR_CLUSTERS = """
    SELECT
        tenant_id,
        service_name,
        cluster_id,
        any(representative_message) AS sample_message,
        sum(event_count) AS error_count,
        '' AS first_seen,
        '' AS last_seen
    FROM observability.log_clusters
    WHERE if(tenant_id = '', 'default', tenant_id) = ?
    GROUP BY tenant_id, service_name, cluster_id
    ORDER BY error_count DESC
    LIMIT 10
    """;

  public static final String ANOMALIES_LAST_HOUR = """
    SELECT
        toString(a.id) AS id,
        toString(a.window_start) AS window_start,
        toString(a.window_end) AS window_end,
        tenant_id,
        service_name,
        environment,
        anomaly_score,
        toInt32(is_anomaly) AS is_anomaly,
        rca_text,
        if(isNull(a.rca_generated_at), '', toString(a.rca_generated_at)) AS rca_generated_at,
        feature_json,
        toString(a.created_at) AS created_at
    FROM observability.anomalies a
    WHERE a.window_start >= now() - INTERVAL 1 HOUR
      AND if(a.tenant_id = '', 'default', a.tenant_id) = ?
    ORDER BY a.window_start DESC
    LIMIT 50
    """;

  public static final String ANOMALY_BY_ID = """
    SELECT
        toString(a.id) AS id,
        toString(a.window_start) AS window_start,
        toString(a.window_end) AS window_end,
        tenant_id,
        service_name,
        environment,
        anomaly_score,
        toInt32(is_anomaly) AS is_anomaly,
        rca_text,
        if(isNull(a.rca_generated_at), '', toString(a.rca_generated_at)) AS rca_generated_at,
        feature_json,
        toString(a.created_at) AS created_at
    FROM observability.anomalies a
    WHERE a.id = toUUID(?)
      AND if(a.tenant_id = '', 'default', a.tenant_id) = ?
    LIMIT 1
    """;

  /** Per-service health from logs_hot: error %, p99 (ms), RPS over 5 minutes. */
  public static final String SERVICE_HEALTH_FROM_LOGS_5M = """
    SELECT
        service_name,
        round(
            countIf(
                log_level = 'ERROR'
                OR toUInt16OrZero(attributes['http.status_code']) >= 500
            ) * 100.0 / nullIf(count(), 0),
            3
        ) AS error_percent,
        quantile(0.99)(duration_ms) AS p99_ms,
        count() / 300.0 AS rps
    FROM observability.logs_hot
    WHERE timestamp >= now() - INTERVAL 5 MINUTE
      AND if(tenant_id = '', 'default', tenant_id) = ?
    GROUP BY service_name
    ORDER BY error_percent DESC
    """;

  /**
   * Seconds from anomaly window_start to created_at (pipeline detection lag) for the latest anomaly row.
   */
  public static final String TIME_TO_DETECT_LAST_INCIDENT = """
    SELECT
        toString(a.id) AS id,
        toUInt32(greatest(0, dateDiff('second', a.window_start, a.created_at))) AS detect_seconds,
        a.service_name,
        toString(a.created_at) AS created_at
    FROM observability.anomalies a
    WHERE a.is_anomaly = 1
      AND if(a.tenant_id = '', 'default', a.tenant_id) = ?
    ORDER BY a.created_at DESC
    LIMIT 1
    """;

  /**
   * Substring search in message. Binds: lookback_seconds (validated int), service_name, message_substring,
   * limit.
   */
  public static final String LOGS_SEARCH_BY_SERVICE_MESSAGE = """
    SELECT
        toString(timestamp) AS timestamp,
        service_name,
        log_level,
        message,
        anomaly_score,
        trace_id
    FROM observability.logs_hot
    WHERE timestamp >= now() - toIntervalSecond(?)
      AND service_name = ?
      AND positionCaseInsensitive(message, ?) > 0
      AND if(tenant_id = '', 'default', tenant_id) = ?
    ORDER BY timestamp DESC
    LIMIT ?
    """;

  /**
   * Same as {@link #LOGS_SEARCH_BY_SERVICE_MESSAGE} with log_level filter. Binds: lookback_seconds,
   * service_name, message_substring, log_level, limit.
   */
  public static final String LOGS_SEARCH_BY_SERVICE_MESSAGE_LEVEL = """
    SELECT
        toString(timestamp) AS timestamp,
        service_name,
        log_level,
        message,
        anomaly_score,
        trace_id
    FROM observability.logs_hot
    WHERE timestamp >= now() - toIntervalSecond(?)
      AND service_name = ?
      AND positionCaseInsensitive(message, ?) > 0
      AND log_level = ?
      AND if(tenant_id = '', 'default', tenant_id) = ?
    ORDER BY timestamp DESC
    LIMIT ?
    """;

  public static final String LOGS_HOT_BY_SERVICE_ENV_AND_WINDOW = """
    SELECT
        toString(lh.timestamp) AS timestamp,
        lh.service_name,
        lh.log_level,
        lh.message,
        lh.anomaly_score,
        lh.trace_id
    FROM observability.logs_hot lh
    WHERE lh.service_name = ?
      AND lh.timestamp >= toDateTime(?)
      AND lh.timestamp < toDateTime(?)
      AND if(lh.tenant_id = '', 'default', lh.tenant_id) = ?
    ORDER BY lh.timestamp DESC
    LIMIT ?
    """;

  /**
   * Latest service_metrics row per (service_name, environment) within lookback window.
   */
  public static final String SERVICE_METRICS_RECENT_PER_SERVICE = """
    SELECT
        service_name,
        environment,
        toString(latest_window_start) AS window_start,
        error_rate,
        ai_anomaly_score,
        log_volume
    FROM (
        SELECT
            service_name,
            environment,
            max(sm.window_start) AS latest_window_start,
            argMax(sm.error_rate, sm.window_start) AS error_rate,
            argMax(sm.ai_anomaly_score, sm.window_start) AS ai_anomaly_score,
            argMax(sm.log_volume, sm.window_start) AS log_volume
        FROM observability.service_metrics AS sm
        WHERE sm.window_start >= now() - INTERVAL 24 HOUR
          AND if(sm.tenant_id = '', 'default', sm.tenant_id) = ?
        GROUP BY service_name, environment
    )
    ORDER BY error_rate DESC
    LIMIT 50
    """;

  public static final String LOGS_HOT_RECENT = """
    SELECT
        toString(timestamp) AS timestamp,
        service_name,
        log_level,
        message,
        cluster_id,
        anomaly_score,
        duration_ms,
        trace_id,
        span_id
    FROM observability.logs_hot
    WHERE if(tenant_id = '', 'default', tenant_id) = ?
    ORDER BY timestamp DESC
    LIMIT ?
    """;
}
