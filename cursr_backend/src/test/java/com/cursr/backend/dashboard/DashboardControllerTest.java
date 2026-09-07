package com.cursr.backend.dashboard;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
class DashboardControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private DashboardQueryService dashboardQueryService;

  @MockBean
  private DashboardSummaryService dashboardSummaryService;

  @MockBean
  private RedisTemplate<String, Object> redisTemplate;

  @MockBean
  private ValueOperations<String, Object> valueOperations;

  @Test
  void errorRateReturnsOkAndArray() throws Exception {
    when(dashboardQueryService.errorRateLast5Min())
      .thenReturn(List.of(Map.of("service_name", "svc", "errors", 1L, "total", 10L, "error_rate", 0.1)));

    mockMvc.perform(get("/api/dashboard/error-rate"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].service_name").value("svc"));
  }

  @Test
  void traceByIdReturnsOk() throws Exception {
    Map<String, Object> combined = new LinkedHashMap<>();
    combined.put(
        "spans",
        List.of(
            Map.of(
                "trace_id",
                "abc123",
                "span_id",
                "s1",
                "span_name",
                "GET /x",
                "duration_ms",
                12)));
    combined.put(
        "logs",
        List.of(Map.of("trace_id", "abc123", "service_name", "svc", "log_level", "INFO", "message", "hi")));
    when(dashboardQueryService.traceCombined(eq("abc123"))).thenReturn(combined);

    mockMvc
        .perform(get("/api/dashboard/trace/abc123"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.spans[0].trace_id").value("abc123"))
        .andExpect(jsonPath("$.logs[0].service_name").value("svc"));
  }

  @Test
  void summaryReturnsEtagAndSupportsNotModified() throws Exception {
    Map<String, Object> summary = Map.of("generatedAt", "2026-01-01T00:00:00Z");
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.get(DashboardCacheKeys.forTenant(DashboardCacheKeys.SUMMARY))).thenReturn(summary);

    String eTag = "\"" + Integer.toHexString(summary.hashCode()) + "\"";

    mockMvc.perform(get("/api/dashboard/summary"))
      .andExpect(status().isOk())
      .andExpect(header().string("ETag", eTag))
      .andExpect(jsonPath("$.generatedAt").value("2026-01-01T00:00:00Z"));

    mockMvc.perform(get("/api/dashboard/summary").header("If-None-Match", eTag))
      .andExpect(status().isNotModified())
      .andExpect(header().string("ETag", eTag));
  }

  @Test
  void serviceMetricsRecentReturnsOk() throws Exception {
    when(dashboardQueryService.serviceMetricsRecent())
        .thenReturn(
            List.of(
                Map.of(
                    "service_name",
                    "payment-api",
                    "environment",
                    "prod",
                    "window_start",
                    "2026-04-03 12:00:00",
                    "error_rate",
                    0.12,
                    "ai_anomaly_score",
                    0.55,
                    "log_volume",
                    1000L)));

    mockMvc
        .perform(get("/api/dashboard/service-metrics-recent"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].service_name").value("payment-api"))
        .andExpect(jsonPath("$[0].environment").value("prod"))
        .andExpect(jsonPath("$[0].error_rate").value(0.12));
  }

  @Test
  void anomalyByIdReturnsRow() throws Exception {
    when(dashboardQueryService.anomalyById(eq("6ba7b810-9dad-11d1-80b4-00c04fd430c8")))
        .thenReturn(
            new LinkedHashMap<>(
                Map.of(
                    "id",
                    "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
                    "service_name",
                    "payment-api",
                    "environment",
                    "prod",
                    "window_start",
                    "2026-04-03 10:00:00",
                    "window_end",
                    "2026-04-03 10:00:30",
                    "anomaly_score",
                    0.88,
                    "is_anomaly",
                    1,
                    "rca_text",
                    "RCA sample.",
                    "rca_generated_at",
                    "2026-04-03 10:01:00")));

    mockMvc
        .perform(get("/api/dashboard/anomalies/6ba7b810-9dad-11d1-80b4-00c04fd430c8"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.service_name").value("payment-api"))
        .andExpect(jsonPath("$.rca_generated_at").value("2026-04-03 10:01:00"));
  }

  @Test
  void anomalyByIdReturns404WhenMissing() throws Exception {
    when(dashboardQueryService.anomalyById(eq("missing"))).thenReturn(Map.of());

    mockMvc.perform(get("/api/dashboard/anomalies/missing")).andExpect(status().isNotFound());
  }

  @Test
  void anomalyLogsReturnsArray() throws Exception {
    when(dashboardQueryService.logsForAnomalyWindow(eq("6ba7b810-9dad-11d1-80b4-00c04fd430c8"), eq(50)))
        .thenReturn(
            List.of(
                Map.of(
                    "timestamp",
                    "2026-04-03 10:00:15",
                    "service_name",
                    "payment-api",
                    "log_level",
                    "ERROR",
                    "message",
                    "deadlock",
                    "anomaly_score",
                    1.0,
                    "trace_id",
                    "t1")));

    mockMvc
        .perform(get("/api/dashboard/anomalies/6ba7b810-9dad-11d1-80b4-00c04fd430c8/logs").param("limit", "50"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].message").value("deadlock"));
  }

  @Test
  void kpisReturnsOk() throws Exception {
    when(dashboardSummaryService.buildKpisOnly())
        .thenReturn(
            new LinkedHashMap<>(
                Map.of(
                    "errorRate",
                    Map.of("currentPercent", 0.8, "window", "5m"),
                    "p99LatencyMs",
                    Map.of("value", 120.0, "health", "healthy"))));

    mockMvc
        .perform(get("/api/dashboard/kpis"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.errorRate.currentPercent").value(0.8))
        .andExpect(jsonPath("$.p99LatencyMs.health").value("healthy"));
  }

  @Test
  void logsSearchReturnsBadRequestWhenMessageMissing() throws Exception {
    mockMvc
        .perform(get("/api/dashboard/logs").param("service_name", "svc").param("message", "  "))
        .andExpect(status().isBadRequest());
  }

  @Test
  void logsSearchReturnsRows() throws Exception {
    when(dashboardQueryService.searchLogsByServiceMessage(eq("payment"), eq("pool"), eq(30), eq(null), eq(50)))
        .thenReturn(
            List.of(
                Map.of(
                    "timestamp",
                    "2026-04-03 10:00:00",
                    "service_name",
                    "payment",
                    "log_level",
                    "ERROR",
                    "message",
                    "DB connection pool exhausted",
                    "trace_id",
                    "t1")));

    mockMvc
        .perform(
            get("/api/dashboard/logs")
                .param("service_name", "payment")
                .param("message", "pool")
                .param("minutes", "30")
                .param("limit", "50"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].message").value("DB connection pool exhausted"));
  }

  @Test
  void summaryReturnsNestedAnomaliesAndServiceMetrics() throws Exception {
    Map<String, Object> summary = new LinkedHashMap<>();
    summary.put("generatedAt", "2026-01-01T00:00:00Z");
    summary.put(
        "anomalies",
        List.of(
            Map.of(
                "id",
                "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
                "service_name",
                "payment-api",
                "environment",
                "prod",
                "is_anomaly",
                1)));
    summary.put(
        "serviceMetricsRecent",
        List.of(
            Map.of(
                "service_name",
                "payment-api",
                "environment",
                "prod",
                "error_rate",
                0.2,
                "ai_anomaly_score",
                0.7,
                "log_volume",
                500L,
                "window_start",
                "2026-04-03 11:00:00")));

    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.get(DashboardCacheKeys.forTenant(DashboardCacheKeys.SUMMARY))).thenReturn(summary);

    mockMvc
        .perform(get("/api/dashboard/summary"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.anomalies[0].service_name").value("payment-api"))
        .andExpect(jsonPath("$.serviceMetricsRecent[0].error_rate").value(0.2));
  }
}
