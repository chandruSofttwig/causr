package com.cursr.backend.dashboard;

import com.cursr.backend.security.TenantContext;

public final class DashboardCacheKeys {

  private DashboardCacheKeys() {}

  public static final String ERROR_RATE = "dashboard:errorRate";
  public static final String LATENCY = "dashboard:latency";
  public static final String TOP_ERRORS = "dashboard:topErrors";
  public static final String RISK = "dashboard:risk";
  public static final String ERROR_CLUSTERS = "dashboard:errorClusters";
  public static final String ANOMALIES = "dashboard:anomalies";
  public static final String SERVICE_METRICS_RECENT = "dashboard:serviceMetricsRecent";
  public static final String SUMMARY = "dashboard:summary";

  public static final String ERROR_RATE_GLOBAL = "dashboard:errorRateGlobal";
  public static final String P99_GLOBAL = "dashboard:p99Global";
  public static final String RPM_GLOBAL = "dashboard:rpmGlobal";
  public static final String SERVICE_HEALTH_LOGS = "dashboard:serviceHealthLogs";
  public static final String LAST_ANOMALY_TTD = "dashboard:lastAnomalyTtd";

  public static String forTenant(String baseKey) {
    return forTenant(baseKey, TenantContext.get());
  }

  public static String forTenant(String baseKey, String tenantId) {
    return baseKey + ":" + TenantContext.normalize(tenantId);
  }
}
