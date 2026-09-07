package com.example.logprocessor.clickhouse;

import com.example.logprocessor.config.ClickHouseProperties;
import com.example.logprocessor.model.RawLogEvent;
import com.example.logprocessor.model.RawSpanEvent;
import com.example.logprocessor.streams.ServiceFeatureRow;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class ClickHouseService {

  private static final DateTimeFormatter ISO_LOCAL = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
  private static final DateTimeFormatter CH_DATETIME =
      new DateTimeFormatterBuilder().appendPattern("yyyy-MM-dd HH:mm:ss").toFormatter();

  private static final String INSERT_COLUMNS =
      " (timestamp, tenant_id, service_name, log_level, message, trace_id, span_id, "
          + "duration_ms, anomaly_score, cluster_id, attributes) "
          + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

  private final JdbcTemplate jdbcTemplate;
  private final String hotTable;
  private final String coldTable;
  private final String serviceMetricsTable;
  private final String anomaliesTable;
  private final String spansTable;

  public ClickHouseService(JdbcTemplate jdbcTemplate, ClickHouseProperties properties) {
    this.jdbcTemplate = jdbcTemplate;
    this.hotTable =
        ClickHouseIdentifiers.resolveTableName(properties.tableLogsHot(), "logs_hot");
    this.coldTable =
        ClickHouseIdentifiers.resolveTableName(properties.tableLogsCold(), "logs_cold");
    this.serviceMetricsTable =
        ClickHouseIdentifiers.resolveTableName(properties.tableServiceMetrics(), "service_metrics");
    this.anomaliesTable =
        ClickHouseIdentifiers.resolveTableName(properties.tableAnomalies(), "anomalies");
    this.spansTable =
        ClickHouseIdentifiers.resolveTableName(properties.tableSpans(), "spans");
  }

  private static final String INSERT_SPAN_COLUMNS =
      " (start_time, trace_id, span_id, parent_span_id, tenant_id, service_name, environment, "
          + "span_name, span_kind, status_code, duration_ms, attributes) "
          + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

  public void insertSpan(RawSpanEvent event) {
    Timestamp startTs = startTimeFromUnixNano(event.start_time_unix_nano);
    String traceId = nullToEmpty(event.trace_id);
    String spanId = nullToEmpty(event.span_id);
    String parentSpanId = nullToEmpty(event.parent_span_id);
    String tenantId = nullToEmpty(event.tenant_id);
    String serviceName = nullToEmpty(event.service_name);
    String environment = nullToEmpty(event.environment);
    String spanName = nullToEmpty(event.span_name);
    String spanKind = nullToEmpty(event.span_kind);
    String statusCode = nullToEmpty(event.status_code);
    int durationMs = Math.max(0, event.duration_ms);
    Map<String, String> attrs = event.attributes != null ? event.attributes : Collections.emptyMap();

    String sql = "INSERT INTO " + spansTable + INSERT_SPAN_COLUMNS;
    jdbcTemplate.update(
        sql,
        startTs,
        traceId,
        spanId,
        parentSpanId,
        tenantId,
        serviceName,
        environment,
        spanName,
        spanKind,
        statusCode,
        durationMs,
        new HashMap<>(attrs));
  }

  private static Timestamp startTimeFromUnixNano(long timeUnixNano) {
    if (timeUnixNano <= 0) {
      return Timestamp.from(Instant.now());
    }
    long seconds = timeUnixNano / 1_000_000_000L;
    int nanos = (int) (timeUnixNano % 1_000_000_000L);
    return Timestamp.from(Instant.ofEpochSecond(seconds, nanos));
  }

  public void insertHot(RawLogEvent event) {
    insert(event, hotTable);
  }

  public void insertCold(RawLogEvent event) {
    insert(event, coldTable);
  }

  private void insert(RawLogEvent event, String table) {
    Timestamp ts = Timestamp.valueOf(parseTimestamp(event.timestamp));
    String tenantId = nullToEmpty(event.tenant_id);
    String serviceName = nullToEmpty(event.service_name);
    String logLevel = nullToEmpty(event.log_level);
    String message = nullToEmpty(event.message);
    String traceId = nullToEmpty(event.trace_id);
    String spanId = nullToEmpty(event.span_id);
    int durationMs = event.duration_ms != null ? Math.max(0, event.duration_ms) : 0;
    float anomalyScore = event.anomaly_score != null ? event.anomaly_score : 0f;
    String clusterId = nullToEmpty(event.cluster_id);
    Map<String, String> attrs = event.attributes != null ? event.attributes : Collections.emptyMap();

    String sql = "INSERT INTO " + table + INSERT_COLUMNS;

    jdbcTemplate.update(
        sql,
        ts,
        tenantId,
        serviceName,
        logLevel,
        message,
        traceId,
        spanId,
        durationMs,
        anomalyScore,
        clusterId,
        new HashMap<>(attrs));
  }

  private static LocalDateTime parseTimestamp(String raw) {
    if (raw == null || raw.isBlank()) {
      return LocalDateTime.now(ZoneOffset.UTC);
    }
    String trimmed = raw.trim();
    try {
      return Instant.parse(trimmed).atZone(ZoneOffset.UTC).toLocalDateTime();
    } catch (DateTimeParseException ignored) {
      // fall through
    }
    try {
      return LocalDateTime.parse(trimmed, ISO_LOCAL);
    } catch (DateTimeParseException ignored) {
      // fall through
    }
    return LocalDateTime.now(ZoneOffset.UTC);
  }

  public void insertServiceMetrics(ServiceFeatureRow row) {
    String sql =
        "INSERT INTO "
            + serviceMetricsTable
            + " (window_start, window_end, tenant_id, service_name, environment, "
            + "log_volume, error_volume, error_rate, p99_latency_ms, unique_error_types, "
            + "new_error_types, silence_flag, deployment_flag, time_of_day_sin, time_of_day_cos, "
            + "ai_anomaly_score) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    jdbcTemplate.update(
        sql,
        Timestamp.valueOf(row.windowStartUtc()),
        Timestamp.valueOf(row.windowEndUtc()),
        row.tenantId(),
        row.serviceName(),
        row.environment(),
        row.logVolume(),
        row.errorVolume(),
        row.errorRate(),
        row.p99LatencyMs(),
        row.uniqueErrorTypes(),
        row.newErrorTypes(),
        row.silenceFlag(),
        row.deploymentFlag(),
        row.timeOfDaySin(),
        row.timeOfDayCos(),
        row.aiAnomalyScore());
  }

  public void insertAnomaly(
      UUID id,
      LocalDateTime windowStartUtc,
      LocalDateTime windowEndUtc,
      String tenantId,
      String serviceName,
      String environment,
      float anomalyScore,
      boolean isAnomaly,
      String featureJson) {
    String sql =
        "INSERT INTO "
            + anomaliesTable
            + " (id, window_start, window_end, tenant_id, service_name, environment, "
            + "anomaly_score, is_anomaly, feature_json, rca_text) "
            + "VALUES (?, toDateTime(?), toDateTime(?), ?, ?, ?, ?, ?, ?, '')";
    jdbcTemplate.update(
        sql,
        id.toString(),
        CH_DATETIME.format(windowStartUtc.withNano(0)),
        CH_DATETIME.format(windowEndUtc.withNano(0)),
        nullToEmpty(tenantId),
        nullToEmpty(serviceName),
        nullToEmpty(environment),
        anomalyScore,
        isAnomaly ? (byte) 1 : (byte) 0,
        nullToEmpty(featureJson));
  }

  private static String nullToEmpty(String s) {
    return s == null ? "" : s;
  }
}
