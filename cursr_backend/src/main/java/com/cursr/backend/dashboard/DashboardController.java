package com.cursr.backend.dashboard;

import java.util.List;
import java.util.Map;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

  private final DashboardQueryService dashboardQueryService;
  private final DashboardSummaryService dashboardSummaryService;
  private final RedisTemplate<String, Object> redisTemplate;

  public DashboardController(
      DashboardQueryService dashboardQueryService,
      DashboardSummaryService dashboardSummaryService,
      RedisTemplate<String, Object> redisTemplate) {
    this.dashboardQueryService = dashboardQueryService;
    this.dashboardSummaryService = dashboardSummaryService;
    this.redisTemplate = redisTemplate;
  }

  @GetMapping("/error-rate")
  public List<Map<String, Object>> errorRate() {
    return dashboardQueryService.errorRateLast5Min();
  }

  @GetMapping("/p95-latency")
  public List<Map<String, Object>> p95Latency() {
    return dashboardQueryService.p95LatencyLast5Min();
  }

  @GetMapping("/top-errors")
  public List<Map<String, Object>> topErrors() {
    return dashboardSummaryService.buildTopErrorsForApi();
  }

  @GetMapping("/logs")
  public ResponseEntity<List<Map<String, Object>>> searchLogs(
      @RequestParam("service_name") String serviceName,
      @RequestParam("message") String message,
      @RequestParam(value = "minutes", required = false, defaultValue = "30") int minutes,
      @RequestParam(value = "log_level", required = false) String logLevel,
      @RequestParam(value = "limit", required = false, defaultValue = "50") int limit) {
    if (serviceName == null || serviceName.isBlank() || message == null || message.isBlank()) {
      return ResponseEntity.badRequest().build();
    }
    return ResponseEntity.ok(
        dashboardQueryService.searchLogsByServiceMessage(serviceName, message, minutes, logLevel, limit));
  }

  @GetMapping("/kpis")
  public Map<String, Object> kpis() {
    return dashboardSummaryService.buildKpisOnly();
  }

  @GetMapping("/trace/{traceId}")
  public ResponseEntity<Map<String, Object>> traceById(@PathVariable String traceId) {
    if (traceId == null || traceId.isBlank()) {
      return ResponseEntity.badRequest().build();
    }
    return ResponseEntity.ok(dashboardQueryService.traceCombined(traceId));
  }

  @GetMapping("/failure-risk")
  public List<Map<String, Object>> failureRisk() {
    return dashboardQueryService.failureRiskLast10Min();
  }

  @GetMapping("/error-clusters")
  public List<Map<String, Object>> errorClusters() {
    return dashboardQueryService.topErrorClusters();
  }

  @GetMapping("/service-metrics-recent")
  public List<Map<String, Object>> serviceMetricsRecent() {
    return dashboardQueryService.serviceMetricsRecent();
  }

  @GetMapping("/anomalies/{id}")
  public ResponseEntity<Map<String, Object>> anomalyById(@PathVariable String id) {
    if (id == null || id.isBlank()) {
      return ResponseEntity.badRequest().build();
    }
    Map<String, Object> row = dashboardQueryService.anomalyById(id);
    if (row.isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(row);
  }

  @GetMapping("/anomalies/{id}/logs")
  public ResponseEntity<List<Map<String, Object>>> anomalyLogs(
      @PathVariable String id,
      @RequestParam(value = "limit", required = false, defaultValue = "50") int limit) {
    if (id == null || id.isBlank()) {
      return ResponseEntity.badRequest().build();
    }
    return ResponseEntity.ok(dashboardQueryService.logsForAnomalyWindow(id, limit));
  }

  @GetMapping("/recent-logs")
  public List<Map<String, Object>> recentLogs(
      @RequestParam(value = "limit", required = false, defaultValue = "100") int limit) {
    return dashboardQueryService.recentLogs(limit);
  }

  @GetMapping("/summary")
  public ResponseEntity<?> summary(
      @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch) {
    Map<String, Object> summary = cachedSummary();
    String eTag = "\"" + Integer.toHexString(summary.hashCode()) + "\"";
    if (eTag.equals(ifNoneMatch)) {
      return ResponseEntity.status(304).eTag(eTag).build();
    }
    return ResponseEntity.ok()
        .eTag(eTag)
        .body(summary);
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> cachedSummary() {
    Object raw = redisTemplate.opsForValue().get(DashboardCacheKeys.forTenant(DashboardCacheKeys.SUMMARY));
    if (raw instanceof Map<?, ?> map) {
      return (Map<String, Object>) map;
    }
    return dashboardSummaryService.buildSummarySnapshot();
  }
}
