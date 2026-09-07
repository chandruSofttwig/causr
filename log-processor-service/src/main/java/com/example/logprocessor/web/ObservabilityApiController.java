package com.example.logprocessor.web;

import com.example.logprocessor.clickhouse.ClickHouseIdentifiers;
import com.example.logprocessor.config.ClickHouseProperties;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ObservabilityApiController {

  private final JdbcTemplate jdbcTemplate;
  private final String hotTable;
  private final String anomaliesTable;
  private final String clusterTable;

  public ObservabilityApiController(JdbcTemplate jdbcTemplate, ClickHouseProperties properties) {
    this.jdbcTemplate = jdbcTemplate;
    this.hotTable =
        ClickHouseIdentifiers.resolveTableName(properties.tableLogsHot(), "logs_hot");
    this.anomaliesTable =
        ClickHouseIdentifiers.resolveTableName(properties.tableAnomalies(), "anomalies");
    this.clusterTable = "log_clusters";
  }

  @GetMapping("/api/logs/recent")
  public List<Map<String, Object>> recentLogs() {
    String sql =
        "SELECT timestamp, service_name, log_level, message, cluster_id, anomaly_score, "
            + "duration_ms, trace_id, span_id "
            + "FROM "
            + hotTable
            + " ORDER BY timestamp DESC LIMIT 100";
    return jdbcTemplate.queryForList(sql);
  }

  @GetMapping("/api/anomalies")
  public List<Map<String, Object>> anomalies() {
    String sql =
        "SELECT id, window_start, window_end, service_name, environment, anomaly_score, "
            + "is_anomaly, rca_text, created_at FROM "
            + anomaliesTable
            + " ORDER BY anomaly_score ASC, window_start DESC LIMIT 200";
    return jdbcTemplate.queryForList(sql);
  }

  @GetMapping("/api/clusters")
  public List<Map<String, Object>> clusters() {
    String sql =
        "SELECT tenant_id, cluster_id, representative_message, event_count FROM "
            + clusterTable
            + " ORDER BY event_count DESC LIMIT 200";
    return jdbcTemplate.queryForList(sql);
  }
}
