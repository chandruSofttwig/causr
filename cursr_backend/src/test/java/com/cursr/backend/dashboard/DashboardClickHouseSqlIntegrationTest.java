package com.cursr.backend.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.clickhouse.ClickHouseContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Executes {@link DashboardSql} against a real ClickHouse (Docker). Requires Docker; skipped when
 * unavailable ({@code disabledWithoutDocker}).
 */
@Testcontainers(disabledWithoutDocker = true)
class DashboardClickHouseSqlIntegrationTest {

  @Container
  private static final ClickHouseContainer CLICKHOUSE =
      new ClickHouseContainer(DockerImageName.parse("clickhouse/clickhouse-server:24.3.6.48"));

  private static JdbcTemplate jdbc;

  @BeforeAll
  static void startDb() {
    CLICKHOUSE.start();
    DriverManagerDataSource ds = new DriverManagerDataSource();
    ds.setDriverClassName("com.clickhouse.jdbc.ClickHouseDriver");
    ds.setUrl(CLICKHOUSE.getJdbcUrl());
    ds.setUsername(CLICKHOUSE.getUsername());
    ds.setPassword(CLICKHOUSE.getPassword());
    jdbc = new JdbcTemplate(ds);
    ClickHouseDashboardTestSupport.createSchema(jdbc);
  }

  @BeforeEach
  void clearTables() {
    ClickHouseDashboardTestSupport.truncateDashboardTables(jdbc);
  }

  @Test
  void serviceMetricsRecentPerService_returnsArgMaxForLatestWindow() {
    ClickHouseDashboardTestSupport.seedServiceMetricsBlastRadius(jdbc);
    List<Map<String, Object>> rows =
        jdbc.queryForList(DashboardSql.SERVICE_METRICS_RECENT_PER_SERVICE, "default");
    assertThat(rows).isNotEmpty();
    Map<String, Object> payment =
        rows.stream()
            .filter(r -> "payment-api".equals(r.get("service_name")) && "prod".equals(r.get("environment")))
            .findFirst()
            .orElseThrow();
    assertThat(f(payment.get("error_rate"))).isCloseTo(0.44f, within(0.01f));
    assertThat(f(payment.get("ai_anomaly_score"))).isCloseTo(0.9f, within(0.01f));
    assertThat(payment.get("window_start")).isNotNull();
  }

  @Test
  void anomaliesLastHour_returnsSeededRow() {
    ClickHouseDashboardTestSupport.seedAnomalyAndWindowLogs(jdbc);
    List<Map<String, Object>> rows = jdbc.queryForList(DashboardSql.ANOMALIES_LAST_HOUR, "default");
    assertThat(rows).hasSize(1);
    Map<String, Object> row = rows.get(0);
    assertThat(row.get("service_name")).isEqualTo("payment-api");
    assertThat(row.get("environment")).isEqualTo("prod");
    assertThat(((Number) row.get("is_anomaly")).intValue()).isEqualTo(1);
    assertThat(String.valueOf(row.get("rca_text"))).contains("Deadlock");
    assertThat(String.valueOf(row.get("rca_generated_at"))).isNotBlank();
  }

  @Test
  void anomalyById_returnsSameRow() {
    ClickHouseDashboardTestSupport.seedAnomalyAndWindowLogs(jdbc);
    List<Map<String, Object>> rows =
        jdbc.queryForList(DashboardSql.ANOMALY_BY_ID, ClickHouseDashboardTestSupport.ANOMALY_ID, "default");
    assertThat(rows).hasSize(1);
    assertThat(rows.get(0).get("service_name")).isEqualTo("payment-api");
  }

  @Test
  void logsHotByServiceAndWindow_returnsEvidenceRows() {
    ClickHouseDashboardTestSupport.seedAnomalyAndWindowLogs(jdbc);
    List<Map<String, Object>> anomalyRow =
        jdbc.queryForList(DashboardSql.ANOMALY_BY_ID, ClickHouseDashboardTestSupport.ANOMALY_ID, "default");
    String windowStart = String.valueOf(anomalyRow.get(0).get("window_start"));
    String windowEnd = String.valueOf(anomalyRow.get(0).get("window_end"));
    List<Map<String, Object>> logs =
        jdbc.queryForList(
            DashboardSql.LOGS_HOT_BY_SERVICE_ENV_AND_WINDOW,
            "payment-api",
            windowStart,
            windowEnd,
            "default",
            50);
    assertThat(logs).hasSizeGreaterThanOrEqualTo(2);
    assertThat(logs.stream().map(r -> String.valueOf(r.get("message"))))
        .anyMatch(m -> m.contains("deadlock"));
  }

  @Test
  void errorRateLast5Min_groupsServices() {
    ClickHouseDashboardTestSupport.seedLogsForDashboardQueries(jdbc);
    List<Map<String, Object>> rows = jdbc.queryForList(DashboardSql.ERROR_RATE_LAST_5_MIN, "default");
    assertThat(rows).isNotEmpty();
    Map<String, Object> payment =
        rows.stream()
            .filter(r -> "payment-api".equals(r.get("service_name")))
            .findFirst()
            .orElseThrow();
    assertThat(((Number) payment.get("errors")).longValue()).isGreaterThanOrEqualTo(3L);
    assertThat(((Number) payment.get("total")).longValue()).isGreaterThanOrEqualTo(4L);
    assertThat(((Number) payment.get("error_rate")).doubleValue()).isCloseTo(60.0, within(0.1));
  }

  @Test
  void p95LatencyLast5Min_exposesP99() {
    ClickHouseDashboardTestSupport.seedLogsForDashboardQueries(jdbc);
    List<Map<String, Object>> rows = jdbc.queryForList(DashboardSql.P95_LATENCY_LAST_5_MIN, "default");
    assertThat(rows).isNotEmpty();
    Map<String, Object> payment =
        rows.stream()
            .filter(r -> "payment-api".equals(r.get("service_name")))
            .findFirst()
            .orElseThrow();
    assertThat(((Number) payment.get("p99_latency")).doubleValue()).isGreaterThan(0);
  }

  @Test
  void topErrors30mWithTrend_returnsErrorMessages() {
    ClickHouseDashboardTestSupport.seedLogsForDashboardQueries(jdbc);
    List<Map<String, Object>> rows = jdbc.queryForList(DashboardSql.TOP_ERRORS_30M_WITH_TREND, "default", "default");
    assertThat(rows).isNotEmpty();
    assertThat(String.valueOf(rows.get(0).get("message"))).isNotBlank();
    assertThat(((Number) rows.get(0).get("error_count")).longValue()).isPositive();
    assertThat(rows.get(0).get("previous_count")).isNotNull();
  }

  @Test
  void errorRateGlobalTwoWindows_returnsCurrentAndPrevious() {
    ClickHouseDashboardTestSupport.seedLogsForDashboardQueries(jdbc);
    List<Map<String, Object>> rows = jdbc.queryForList(DashboardSql.ERROR_RATE_GLOBAL_TWO_WINDOWS, "default", "default");
    assertThat(rows).hasSize(2);
    assertThat(rows.stream().map(r -> String.valueOf(r.get("window_label"))))
        .containsExactlyInAnyOrder("current", "previous");
  }

  @Test
  void serviceHealthFromLogs5m_returnsPerServiceMetrics() {
    ClickHouseDashboardTestSupport.seedLogsForDashboardQueries(jdbc);
    List<Map<String, Object>> rows = jdbc.queryForList(DashboardSql.SERVICE_HEALTH_FROM_LOGS_5M, "default");
    assertThat(rows).isNotEmpty();
    Map<String, Object> payment =
        rows.stream()
            .filter(r -> "payment-api".equals(r.get("service_name")))
            .findFirst()
            .orElseThrow();
    assertThat(((Number) payment.get("p99_ms")).doubleValue()).isGreaterThan(0);
    assertThat(DashboardKpiService.serviceRowStatus(payment)).isEqualTo("RED");
  }

  @Test
  void timeToDetectLastIncident_returnsDetectSeconds() {
    ClickHouseDashboardTestSupport.seedAnomalyAndWindowLogs(jdbc);
    List<Map<String, Object>> rows = jdbc.queryForList(DashboardSql.TIME_TO_DETECT_LAST_INCIDENT, "default");
    assertThat(rows).hasSize(1);
    assertThat(((Number) rows.get(0).get("detect_seconds")).intValue()).isGreaterThanOrEqualTo(0);
  }

  @Test
  void logsSearchByServiceMessage_findsRows() {
    ClickHouseDashboardTestSupport.seedLogsForDashboardQueries(jdbc);
    List<Map<String, Object>> rows =
        jdbc.queryForList(
            DashboardSql.LOGS_SEARCH_BY_SERVICE_MESSAGE,
            30 * 60,
            "payment-api",
            "timeout",
            20);
    assertThat(rows).isNotEmpty();
    assertThat(String.valueOf(rows.get(0).get("message"))).containsIgnoringCase("timeout");
  }

  @Test
  void failureRiskLast10Min_returnsHighRiskService() {
    ClickHouseDashboardTestSupport.seedLogsForDashboardQueries(jdbc);
    List<Map<String, Object>> rows = jdbc.queryForList(DashboardSql.FAILURE_RISK_LAST_10_MIN, "default");
    assertThat(rows).isNotEmpty();
    assertThat(rows.get(0).get("service_name")).isEqualTo("payment-api");
    assertThat(((Number) rows.get(0).get("risk_score")).doubleValue()).isGreaterThan(40);
  }

  @Test
  void topErrorClusters_matchesDashboardColumns() {
    ClickHouseDashboardTestSupport.seedLogClusters(jdbc);
    List<Map<String, Object>> rows = jdbc.queryForList(DashboardSql.TOP_ERROR_CLUSTERS, "default");
    assertThat(rows).hasSizeGreaterThanOrEqualTo(2);
    assertThat(rows.get(0).get("sample_message")).isNotNull();
    assertThat(String.valueOf(rows.get(0).get("service_name"))).isNotBlank();
    assertThat(((Number) rows.get(0).get("error_count")).longValue()).isGreaterThanOrEqualTo(7L);
  }

  @Test
  void traceLogsByTraceId_ordersByTimestamp() {
    ClickHouseDashboardTestSupport.seedLogsForDashboardQueries(jdbc);
    List<Map<String, Object>> rows = jdbc.queryForList(DashboardSql.TRACE_LOGS_BY_TRACE_ID, "trace-pay-1", "default");
    assertThat(rows).hasSize(1);
    assertThat(rows.get(0).get("trace_id")).isEqualTo("trace-pay-1");
  }

  @Test
  void traceSpansByTraceId_returnsSpansForTrace() {
    ClickHouseDashboardTestSupport.seedSpansForDashboardQueries(jdbc);
    List<Map<String, Object>> rows = jdbc.queryForList(DashboardSql.TRACE_SPANS_BY_TRACE_ID, "trace-span-1", "default");
    assertThat(rows).hasSize(2);
    assertThat(rows.stream().map(r -> r.get("span_id")).toList()).contains("sp1", "sp2");
  }

  @Test
  void spansServerRecentCount_positiveWhenServerSpansInserted() {
    ClickHouseDashboardTestSupport.seedSpansForDashboardQueries(jdbc);
    Long c = jdbc.queryForObject(DashboardSql.SPANS_SERVER_RECENT_COUNT, Long.class);
    assertThat(c).isNotNull();
    assertThat(c).isGreaterThan(0L);
  }

  @Test
  void p99GlobalTwoWindowsSpans_returnsRows() {
    ClickHouseDashboardTestSupport.seedSpansForDashboardQueries(jdbc);
    List<Map<String, Object>> rows = jdbc.queryForList(DashboardSql.P99_GLOBAL_TWO_WINDOWS_SPANS, "default", "default");
    assertThat(rows).hasSize(2);
    assertThat(rows.get(0).get("window_label")).isEqualTo("current");
  }

  private static float f(Object v) {
    if (v == null) {
      return Float.NaN;
    }
    if (v instanceof Float f) {
      return f;
    }
    return ((Number) v).floatValue();
  }
}
