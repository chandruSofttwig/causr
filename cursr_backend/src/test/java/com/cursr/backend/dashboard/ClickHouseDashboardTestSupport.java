package com.cursr.backend.dashboard;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * DDL and fixtures for {@link DashboardSql} integration tests against ClickHouse. Table shapes match
 * {@code log-processor-service} schema and {@code docs/clickhouse/log_clusters_mv.sql} (dashboard query).
 */
final class ClickHouseDashboardTestSupport {

  private ClickHouseDashboardTestSupport() {}

  static void createSchema(JdbcTemplate jdbc) {
    for (String stmt : schemaStatements()) {
      jdbc.execute(stmt);
    }
  }

  static void truncateDashboardTables(JdbcTemplate jdbc) {
    for (String table :
        List.of(
            "observability.logs_hot",
            "observability.service_metrics",
            "observability.anomalies",
            "observability.log_clusters",
            "observability.spans")) {
      jdbc.execute("TRUNCATE TABLE IF EXISTS " + table);
    }
  }

  private static List<String> schemaStatements() {
    return List.of(
        "CREATE DATABASE IF NOT EXISTS observability",
        """
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
        """,
        """
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
        """,
        """
        CREATE TABLE IF NOT EXISTS observability.anomalies
        (
            id UUID,
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
        """,
        """
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
        ORDER BY (tenant_id, service_name, cluster_id, representative_message)
        """,
        """
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
        """);
  }

  /** Fixed UUID for deterministic anomaly-by-id tests. */
  static final String ANOMALY_ID = "6ba7b810-9dad-11d1-80b4-00c04fd430c8";

  static void seedLogsForDashboardQueries(JdbcTemplate jdbc) {
    jdbc.update(
        """
        INSERT INTO observability.logs_hot
        (timestamp, tenant_id, service_name, log_level, message, trace_id, span_id, duration_ms, anomaly_score, cluster_id, attributes)
        VALUES
          (now() - 60, 't', 'payment-api', 'ERROR', 'timeout contacting upstream', 'trace-pay-1', 's1', 200, 0.4, 'cl1', map('k','v')),
          (now() - 90, 't', 'payment-api', 'ERROR', 'second timeout path', 'trace-pay-1b', 's1b', 220, 0.3, 'cl1', map()),
          (now() - 100, 't', 'payment-api', 'ERROR', 'third timeout burst', 'trace-pay-1c', 's1c', 210, 0.3, 'cl1', map()),
          (now() - 120, 't', 'payment-api', 'INFO', 'ok', 'trace-pay-2', 's2', 10, 0.0, '', map()),
          (now() - 180, 't', 'payment-api', 'WARN', 'connection pool high', 'trace-pay-3', 's3', 50, 0.0, '', map()),
          (now() - 240, 't', 'checkout-api', 'ERROR', 'connection refused to db', 'trace-ch-1', 's4', 300, 0.6, 'cl2', map())
        """);
  }

  static void seedServiceMetricsBlastRadius(JdbcTemplate jdbc) {
    jdbc.update(
        """
        INSERT INTO observability.service_metrics
        (window_start, window_end, tenant_id, service_name, environment, log_volume, error_volume, error_rate, p99_latency_ms,
         unique_error_types, new_error_types, silence_flag, deployment_flag, time_of_day_sin, time_of_day_cos, ai_anomaly_score)
        VALUES
          (now() - 3600, now() - 3570, 't', 'payment-api', 'prod', 1000, 100, 0.10, 50, 3, 0, 0, 0, 0, 0, 0.3),
          (now() - 120, now() - 90, 't', 'payment-api', 'prod', 900, 400, 0.44, 80, 5, 1, 0, 0, 0, 0, 0.9),
          (now() - 300, now() - 270, 't', 'checkout-api', 'prod', 500, 10, 0.02, 40, 1, 0, 0, 0, 0, 0, 0.1)
        """);
  }

  static void seedAnomalyAndWindowLogs(JdbcTemplate jdbc) {
    jdbc.update(
        String.format(
            """
            INSERT INTO observability.anomalies
            (id, window_start, window_end, tenant_id, service_name, environment, anomaly_score, is_anomaly, feature_json, rca_text, rca_generated_at)
            VALUES
              (toUUID('%s'), now() - 600, now() - 570, 't', 'payment-api', 'prod', 0.88, 1, '{}', 'Deadlock on inventory.', now() - 580)
            """,
            ANOMALY_ID));
    jdbc.update(
        """
        INSERT INTO observability.logs_hot
        (timestamp, tenant_id, service_name, log_level, message, trace_id, span_id, duration_ms, anomaly_score, cluster_id, attributes)
        VALUES
          (now() - 590, 't', 'payment-api', 'ERROR', 'inventory deadlock', 'ev-1', 's1', 900, 1.0, '', map()),
          (now() - 580, 't', 'payment-api', 'ERROR', 'retry failed', 'ev-2', 's2', 800, 0.9, '', map())
        """);
  }

  static void seedSpansForDashboardQueries(JdbcTemplate jdbc) {
    jdbc.update(
        """
        INSERT INTO observability.spans
        (start_time, trace_id, span_id, parent_span_id, tenant_id, service_name, environment,
         span_name, span_kind, status_code, duration_ms, attributes)
        VALUES
          (now64(3), 'trace-span-1', 'sp1', '', 't', 'payment-api', 'prod', 'GET /pay', 'SERVER', 'OK', 45, map()),
          (now64(3), 'trace-span-1', 'sp2', 'sp1', 't', 'payment-api', 'prod', 'internal', 'INTERNAL', 'OK', 10, map()),
          (now64(3), 'trace-pay-1', 'sp3', '', 't', 'payment-api', 'prod', 'POST /x', 'SERVER', 'ERROR', 100, map('http.status_code', '500'))
        """);
  }

  static void seedLogClusters(JdbcTemplate jdbc) {
    jdbc.update(
        """
        INSERT INTO observability.log_clusters
        (tenant_id, service_name, cluster_id, representative_message, event_count)
        VALUES
          ('t', 'payment-api', 'dependency_timeout', 'timeout contacting upstream', 42),
          ('t', 'checkout-api', 'db_deadlock', 'connection refused', 7)
        """);
  }
}
