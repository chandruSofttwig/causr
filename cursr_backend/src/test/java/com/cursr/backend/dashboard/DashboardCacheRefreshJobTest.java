package com.cursr.backend.dashboard;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
class DashboardCacheRefreshJobTest {

  @Mock private JdbcTemplate jdbcTemplate;
  @Mock private RedisTemplate<String, Object> redisTemplate;
  @Mock private ValueOperations<String, Object> valueOps;
  @Mock private DashboardSummaryService dashboardSummaryService;
  @Mock private SimpMessagingTemplate messagingTemplate;

  @BeforeEach
  void setUp() {
    when(redisTemplate.opsForValue()).thenReturn(valueOps);
  }

  @Test
  void refreshAll_writesTenantScopedDashboardKeys() {
    List<Map<String, Object>> row = List.of(Map.of("service_name", "svc"));
    Map<String, Object> summary = Map.of("generatedAt", "2026-01-01T00:00:00Z");

    doReturn(0L).when(jdbcTemplate).queryForObject(anyString(), eq(Long.class), any());
    doAnswer(invocation -> row).when(jdbcTemplate).queryForList(anyString(), (Object[]) any());
    when(dashboardSummaryService.buildSummarySnapshot()).thenReturn(summary);

    DashboardCacheRefreshJob job =
        new DashboardCacheRefreshJob(
            jdbcTemplate, redisTemplate, dashboardSummaryService, messagingTemplate, "default");
    job.refreshAll();

    verify(valueOps)
        .set(eq(DashboardCacheKeys.forTenant(DashboardCacheKeys.ERROR_RATE, "default")), eq(row));
    verify(valueOps)
        .set(eq(DashboardCacheKeys.forTenant(DashboardCacheKeys.SUMMARY, "default")), eq(summary));
    verify(messagingTemplate).convertAndSend(eq("/topic/dashboard/summary/default"), eq(summary));
    verify(messagingTemplate, atLeastOnce())
        .convertAndSend(eq("/topic/dashboard/summary"), eq(summary));
  }
}
