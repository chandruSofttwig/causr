package com.cursr.backend.dashboard;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DashboardSummaryService {

  private final DashboardQueryService dashboardQueryService;
  private final DashboardKpiService dashboardKpiService;

  public DashboardSummaryService(
      DashboardQueryService dashboardQueryService,
      DashboardKpiService dashboardKpiService) {
    this.dashboardQueryService = dashboardQueryService;
    this.dashboardKpiService = dashboardKpiService;
  }

  public Map<String, Object> buildKpisOnly() {
    return dashboardKpiService.buildKpis();
  }

  public List<Map<String, Object>> buildTopErrorsForApi() {
    return enrichTopErrors(dashboardQueryService.topErrors30mWithTrend());
  }

  public Map<String, Object> buildSummarySnapshot() {
    Map<String, Object> summary = new LinkedHashMap<>();
    summary.put("summaryVersion", 3);
    summary.put("generatedAt", Instant.now().toString());
    summary.put("kpis", dashboardKpiService.buildKpis());
    summary.put("errorRate", dashboardQueryService.errorRateLast5Min());
    summary.put("latency", dashboardQueryService.p95LatencyLast5Min());
    summary.put("topErrors", enrichTopErrors(dashboardQueryService.topErrors30mWithTrend()));
    summary.put("serviceHealth", enrichServiceHealth(dashboardQueryService.serviceHealthFromLogs5m()));
    summary.put("risk", dashboardQueryService.failureRiskLast10Min());
    summary.put("errorClusters", dashboardQueryService.topErrorClusters());
    summary.put("anomalies", dashboardQueryService.anomaliesLastHour());
    summary.put("serviceMetricsRecent", dashboardQueryService.serviceMetricsRecent());
    return summary;
  }

  private static List<Map<String, Object>> enrichServiceHealth(List<Map<String, Object>> rows) {
    List<Map<String, Object>> out = new ArrayList<>(rows.size());
    for (Map<String, Object> row : rows) {
      Map<String, Object> m = new LinkedHashMap<>(row);
      m.put("status", DashboardKpiService.serviceRowStatus(row));
      out.add(m);
    }
    return out;
  }

  private static List<Map<String, Object>> enrichTopErrors(List<Map<String, Object>> rows) {
    List<Map<String, Object>> out = new ArrayList<>(rows.size());
    for (Map<String, Object> row : rows) {
      Map<String, Object> m = new LinkedHashMap<>();
      String service = String.valueOf(row.get("service_name"));
      String message = String.valueOf(row.get("message"));
      m.put("service_name", service);
      m.put("message", message);
      m.put("error_message", message);
      long cur = toLong(row.get("error_count"));
      long prev = toLong(row.get("previous_count"));
      m.put("count", cur);
      m.put("error_count", cur);
      m.put("previous_count", prev);
      m.put("trend", trendLabel(cur, prev));
      m.put("logsQuery", buildLogsQueryLink(service, message, 30));
      out.add(m);
    }
    return out;
  }

  private static String trendLabel(long current, long previous) {
    if (current > previous) {
      return "up";
    }
    if (current < previous) {
      return "down";
    }
    return "flat";
  }

  private static long toLong(Object v) {
    if (v == null) {
      return 0L;
    }
    if (v instanceof Number n) {
      return n.longValue();
    }
    try {
      return Long.parseLong(String.valueOf(v));
    } catch (NumberFormatException e) {
      return 0L;
    }
  }

  /**
   * Relative API path for opening logs filtered to this error (UTF-8 encoded query string).
   */
  static String buildLogsQueryLink(String serviceName, String message, int minutes) {
    return "/api/dashboard/logs?service_name="
        + URLEncoder.encode(serviceName, StandardCharsets.UTF_8)
        + "&message="
        + URLEncoder.encode(message, StandardCharsets.UTF_8)
        + "&minutes="
        + minutes;
  }
}
