package com.cursr.backend.dashboard;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Composes headline KPI objects for the dashboard summary.
 *
 * <p>Comparison semantics: error rate and p99 use current 5m vs previous 5m; RPM uses current 1m vs
 * previous 1m. {@code servicesHealthy} overall card color: all green → green; 1–2 non-green
 * services → amber; more than 2 non-green → red.
 */
@Service
public class DashboardKpiService {

  static final String WINDOW_5M = "5m";
  static final String WINDOW_1M = "1m";
  static final String COMPARE_WINDOW_5M = "previous_5m";
  static final String COMPARE_WINDOW_1M = "previous_1m";

  private final DashboardQueryService dashboardQueryService;

  public DashboardKpiService(DashboardQueryService dashboardQueryService) {
    this.dashboardQueryService = dashboardQueryService;
  }

  public Map<String, Object> buildKpis() {
    Map<String, Object> kpis = new LinkedHashMap<>();
    kpis.put("errorRate", buildErrorRateKpi());
    kpis.put("p99LatencyMs", buildP99Kpi());
    kpis.put("requestsPerMinute", buildRpmKpi());
    kpis.put("servicesHealthy", buildServicesHealthyKpi());
    kpis.put("timeToDetect", buildTimeToDetectKpi());
    return kpis;
  }

  private Map<String, Object> buildErrorRateKpi() {
    List<Map<String, Object>> rows = dashboardQueryService.errorRateGlobalWindows();
    double current = pctForLabel(rows, "current");
    double previous = pctForLabel(rows, "previous");
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("currentPercent", round2(current));
    m.put("previousPercent", round2(previous));
    m.put("window", WINDOW_5M);
    m.put("compareWindow", COMPARE_WINDOW_5M);
    m.put("direction", compareDirection(current, previous, true));
    return m;
  }

  private Map<String, Object> buildP99Kpi() {
    List<Map<String, Object>> rows = dashboardQueryService.p99GlobalWindows();
    double current = doubleForLabel(rows, "current", "p99_ms");
    double previous = doubleForLabel(rows, "previous", "p99_ms");
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("value", round1(current));
    m.put("previousValue", round1(previous));
    m.put("health", classifyP99Health(current));
    m.put("window", WINDOW_5M);
    m.put("compareWindow", COMPARE_WINDOW_5M);
    m.put("direction", compareDirection(current, previous, true));
    return m;
  }

  private Map<String, Object> buildRpmKpi() {
    List<Map<String, Object>> rows = dashboardQueryService.rpmGlobalWindows();
    double current = doubleForLabel(rows, "current", "rpm");
    double previous = doubleForLabel(rows, "previous", "rpm");
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("current", round2(current));
    m.put("previous", round2(previous));
    m.put("window", WINDOW_1M);
    m.put("compareWindow", COMPARE_WINDOW_1M);
    m.put("deltaPercent", round1(deltaPercent(current, previous)));
    m.put(
        "direction",
        current > previous ? "up" : current < previous ? "down" : "flat");
    return m;
  }

  private Map<String, Object> buildServicesHealthyKpi() {
    List<Map<String, Object>> rows = dashboardQueryService.serviceHealthFromLogs5m();
    int total = rows.size();
    int healthy = 0;
    int nonGreen = 0;
    for (Map<String, Object> row : rows) {
      String status = serviceRowStatus(row);
      if ("GREEN".equals(status)) {
        healthy++;
      } else {
        nonGreen++;
      }
    }
    String severity;
    if (nonGreen == 0) {
      severity = "green";
    } else if (nonGreen <= 2) {
      severity = "amber";
    } else {
      severity = "red";
    }
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("healthy", healthy);
    m.put("total", total);
    m.put("nonGreen", nonGreen);
    m.put("severity", severity);
    m.put("window", WINDOW_5M);
    return m;
  }

  /** Status for one service row from {@link DashboardSql#SERVICE_HEALTH_FROM_LOGS_5M}. */
  public static String serviceRowStatus(Map<String, Object> row) {
    double err = toDouble(row.get("error_percent"));
    double p99 = toDouble(row.get("p99_ms"));
    if (err > 5.0 || p99 > 800.0) {
      return "RED";
    }
    if (err > 1.0 || p99 > 300.0) {
      return "AMBER";
    }
    return "GREEN";
  }

  private Map<String, Object> buildTimeToDetectKpi() {
    List<Map<String, Object>> rows = dashboardQueryService.lastAnomalyTimeToDetect();
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("windowLabel", "last_incident");
    if (rows.isEmpty()) {
      m.put("lastIncidentSeconds", null);
      m.put("incidentId", null);
      m.put("serviceName", null);
      m.put("createdAt", null);
      m.put("avgDetectionSeconds", null);
      m.put("avgMttrSeconds", null);
      return m;
    }
    Map<String, Object> row = rows.get(0);
    m.put("lastIncidentSeconds", numberToLong(row.get("detect_seconds")));
    m.put("incidentId", stringVal(row.get("id")));
    m.put("serviceName", stringVal(row.get("service_name")));
    m.put("createdAt", stringVal(row.get("created_at")));
    m.put("avgDetectionSeconds", null);
    m.put("avgMttrSeconds", null);
    return m;
  }

  private static String classifyP99Health(double p99Ms) {
    if (Double.isNaN(p99Ms) || p99Ms <= 0) {
      return "healthy";
    }
    if (p99Ms > 500) {
      return "critical";
    }
    if (p99Ms >= 200) {
      return "degraded";
    }
    return "healthy";
  }

  /**
   * @param higherIsWorse when true, {@code up} means current &gt; previous (e.g. error rate, latency).
   */
  private static String compareDirection(double current, double previous, boolean higherIsWorse) {
    if (Double.isNaN(current) || Double.isNaN(previous)) {
      return "flat";
    }
    if (current > previous) {
      return higherIsWorse ? "up" : "down";
    }
    if (current < previous) {
      return higherIsWorse ? "down" : "up";
    }
    return "flat";
  }

  private static double deltaPercent(double current, double previous) {
    if (previous <= 0) {
      return current > 0 ? 100.0 : 0.0;
    }
    return (current - previous) * 100.0 / previous;
  }

  private static double pctForLabel(List<Map<String, Object>> rows, String label) {
    for (Map<String, Object> row : rows) {
      if (label.equals(String.valueOf(row.get("window_label")))) {
        return toDouble(row.get("error_rate_pct"));
      }
    }
    return 0.0;
  }

  private static double doubleForLabel(List<Map<String, Object>> rows, String label, String col) {
    for (Map<String, Object> row : rows) {
      if (label.equals(String.valueOf(row.get("window_label")))) {
        return toDouble(row.get(col));
      }
    }
    return Double.NaN;
  }

  private static double toDouble(Object v) {
    if (v == null) {
      return Double.NaN;
    }
    if (v instanceof Number n) {
      return n.doubleValue();
    }
    try {
      return Double.parseDouble(String.valueOf(v));
    } catch (NumberFormatException e) {
      return Double.NaN;
    }
  }

  private static Long numberToLong(Object v) {
    if (v == null) {
      return null;
    }
    if (v instanceof Number n) {
      return n.longValue();
    }
    try {
      return Long.parseLong(String.valueOf(v));
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private static String stringVal(Object v) {
    return v == null ? null : String.valueOf(v);
  }

  private static double round1(double v) {
    if (Double.isNaN(v)) {
      return 0.0;
    }
    return Math.round(v * 10.0) / 10.0;
  }

  private static double round2(double v) {
    if (Double.isNaN(v)) {
      return 0.0;
    }
    return Math.round(v * 100.0) / 100.0;
  }
}
