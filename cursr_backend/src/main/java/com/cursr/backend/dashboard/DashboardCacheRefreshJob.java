package com.cursr.backend.dashboard;

import com.cursr.backend.security.TenantContext;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class DashboardCacheRefreshJob {

  private static final Logger log = LoggerFactory.getLogger(DashboardCacheRefreshJob.class);

  private final JdbcTemplate jdbcTemplate;
  private final RedisTemplate<String, Object> redisTemplate;
  private final DashboardSummaryService dashboardSummaryService;
  private final SimpMessagingTemplate messagingTemplate;
  private final List<String> tenants;

  public DashboardCacheRefreshJob(
      JdbcTemplate jdbcTemplate,
      RedisTemplate<String, Object> redisTemplate,
      DashboardSummaryService dashboardSummaryService,
      SimpMessagingTemplate messagingTemplate,
      @Value("${app.security.tenants:default}") String tenantsCsv) {
    this.jdbcTemplate = jdbcTemplate;
    this.redisTemplate = redisTemplate;
    this.dashboardSummaryService = dashboardSummaryService;
    this.messagingTemplate = messagingTemplate;
    this.tenants =
        List.of(tenantsCsv.split(",")).stream()
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(TenantContext::normalize)
            .distinct()
            .toList();
  }

  @EventListener(ApplicationReadyEvent.class)
  public void warmOnStartup() {
    refreshAll();
  }

  @Scheduled(fixedRate = 10_000)
  public void updateDashboardCache() {
    refreshAll();
  }

  void refreshAll() {
    for (String tenant : tenants) {
      refreshTenant(tenant);
    }
  }

  private void refreshTenant(String tenant) {
    TenantContext.set(tenant);
    try {
      boolean useSpans = preferSpanMetrics(tenant);
      putIfOk(
          DashboardCacheKeys.ERROR_RATE,
          tenant,
          () ->
              jdbcTemplate.queryForList(
                  useSpans
                      ? DashboardSql.ERROR_RATE_LAST_5_MIN_SPANS
                      : DashboardSql.ERROR_RATE_LAST_5_MIN,
                  tenant));
      putIfOk(
          DashboardCacheKeys.LATENCY,
          tenant,
          () ->
              jdbcTemplate.queryForList(
                  useSpans
                      ? DashboardSql.P95_LATENCY_LAST_5_MIN_SPANS
                      : DashboardSql.P95_LATENCY_LAST_5_MIN,
                  tenant));
      putIfOk(
          DashboardCacheKeys.TOP_ERRORS,
          tenant,
          () -> jdbcTemplate.queryForList(DashboardSql.TOP_ERRORS_30M_WITH_TREND, tenant, tenant));
      putIfOk(
          DashboardCacheKeys.RISK,
          tenant,
          () -> jdbcTemplate.queryForList(DashboardSql.FAILURE_RISK_LAST_10_MIN, tenant));
      putIfOk(
          DashboardCacheKeys.ERROR_CLUSTERS,
          tenant,
          () -> jdbcTemplate.queryForList(DashboardSql.TOP_ERROR_CLUSTERS, tenant));
      putIfOk(
          DashboardCacheKeys.ANOMALIES,
          tenant,
          () -> jdbcTemplate.queryForList(DashboardSql.ANOMALIES_LAST_HOUR, tenant));
      putIfOk(
          DashboardCacheKeys.SERVICE_METRICS_RECENT,
          tenant,
          () -> jdbcTemplate.queryForList(DashboardSql.SERVICE_METRICS_RECENT_PER_SERVICE, tenant));
      putIfOk(
          DashboardCacheKeys.ERROR_RATE_GLOBAL,
          tenant,
          () ->
              jdbcTemplate.queryForList(
                  useSpans
                      ? DashboardSql.ERROR_RATE_GLOBAL_TWO_WINDOWS_SPANS
                      : DashboardSql.ERROR_RATE_GLOBAL_TWO_WINDOWS,
                  tenant,
                  tenant));
      putIfOk(
          DashboardCacheKeys.P99_GLOBAL,
          tenant,
          () ->
              jdbcTemplate.queryForList(
                  useSpans
                      ? DashboardSql.P99_GLOBAL_TWO_WINDOWS_SPANS
                      : DashboardSql.P99_GLOBAL_TWO_WINDOWS,
                  tenant,
                  tenant));
      putIfOk(
          DashboardCacheKeys.RPM_GLOBAL,
          tenant,
          () ->
              jdbcTemplate.queryForList(
                  useSpans
                      ? DashboardSql.RPM_GLOBAL_TWO_WINDOWS_SPANS
                      : DashboardSql.RPM_GLOBAL_TWO_WINDOWS,
                  tenant,
                  tenant));
      putIfOk(
          DashboardCacheKeys.SERVICE_HEALTH_LOGS,
          tenant,
          () ->
              jdbcTemplate.queryForList(
                  useSpans
                      ? DashboardSql.SERVICE_HEALTH_FROM_SPANS_5M
                      : DashboardSql.SERVICE_HEALTH_FROM_LOGS_5M,
                  tenant));
      putIfOk(
          DashboardCacheKeys.LAST_ANOMALY_TTD,
          tenant,
          () -> jdbcTemplate.queryForList(DashboardSql.TIME_TO_DETECT_LAST_INCIDENT, tenant));
      putSummary(tenant);
    } finally {
      TenantContext.clear();
    }
  }

  private boolean preferSpanMetrics(String tenant) {
    try {
      Long c =
          jdbcTemplate.queryForObject(DashboardSql.SPANS_SERVER_RECENT_COUNT, Long.class, tenant);
      return c != null && c > 0;
    } catch (Exception e) {
      return false;
    }
  }

  private void putIfOk(
      String baseKey, String tenant, java.util.concurrent.Callable<List<Map<String, Object>>> query) {
    try {
      List<Map<String, Object>> rows = query.call();
      redisTemplate.opsForValue().set(DashboardCacheKeys.forTenant(baseKey, tenant), rows);
    } catch (Exception e) {
      log.warn(
          "Dashboard cache refresh skipped for key {} tenant {}: {}",
          baseKey,
          tenant,
          e.getMessage());
    }
  }

  private void putSummary(String tenant) {
    try {
      Map<String, Object> summary = dashboardSummaryService.buildSummarySnapshot();
      redisTemplate.opsForValue().set(DashboardCacheKeys.forTenant(DashboardCacheKeys.SUMMARY, tenant), summary);
      messagingTemplate.convertAndSend("/topic/dashboard/summary/" + tenant, summary);
      // Keep legacy topic for older clients on default tenant only.
      if ("default".equals(tenant)) {
        messagingTemplate.convertAndSend("/topic/dashboard/summary", summary);
      }
    } catch (Exception e) {
      log.warn(
          "Dashboard cache refresh skipped for summary tenant {}: {}", tenant, e.getMessage());
    }
  }
}
