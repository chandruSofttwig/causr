package com.example.logprocessor.web;

import com.example.logprocessor.clickhouse.ClickHouseIdentifiers;
import com.example.logprocessor.config.ClickHouseProperties;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SearchController {

  private final JdbcTemplate jdbcTemplate;
  private final String hotTable;

  public SearchController(JdbcTemplate jdbcTemplate, ClickHouseProperties properties) {
    this.jdbcTemplate = jdbcTemplate;
    this.hotTable =
        ClickHouseIdentifiers.resolveTableName(properties.tableLogsHot(), "logs_hot");
  }

  @GetMapping("/api/search")
  public List<Map<String, Object>> search(@RequestParam("q") String q) {
    if (q == null || q.isBlank()) {
      return List.of();
    }
    String needle = "%" + q.trim() + "%";
    String sql =
        "SELECT timestamp, service_name, log_level, message, cluster_id, "
            + "duration_ms, trace_id, span_id "
            + "FROM "
            + hotTable
            + " WHERE message ILIKE ? "
            + "ORDER BY timestamp DESC LIMIT 100";
    return jdbcTemplate.queryForList(sql, needle);
  }
}
