package com.causr.llmrouter.clickhouse;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AnomalyRcaRepository {

  private static final DateTimeFormatter CH_DT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);

  private final JdbcTemplate jdbcTemplate;

  public AnomalyRcaRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public List<Map<String, Object>> findRecentErrorLogs(
      String serviceName, String environment, long windowStartEpochMs, long windowEndEpochMs, int limit) {
    String start = CH_DT.format(Instant.ofEpochMilli(Math.max(0, windowStartEpochMs - 60_000)));
    String end = CH_DT.format(Instant.ofEpochMilli(windowEndEpochMs + 60_000));
    String sql =
        """
        SELECT timestamp, log_level, message, cluster_id, duration_ms, trace_id
        FROM logs_hot
        WHERE service_name = ?
          AND (environment = ? OR ? = '')
          AND timestamp >= toDateTime(?)
          AND timestamp <= toDateTime(?)
          AND (upper(log_level) IN ('ERROR', 'FATAL', 'WARN', 'WARNING')
               OR http_status_code >= 500)
        ORDER BY timestamp DESC
        LIMIT ?
        """;
    String env = environment == null ? "" : environment;
    return jdbcTemplate.queryForList(sql, serviceName, env, env, start, end, limit);
  }

  public void updateRca(UUID anomalyId, String rcaText) {
    String sql =
        """
        ALTER TABLE anomalies
        UPDATE rca_text = ?, rca_generated_at = now()
        WHERE id = ?
        """;
    jdbcTemplate.update(sql, rcaText, anomalyId.toString());
  }
}
