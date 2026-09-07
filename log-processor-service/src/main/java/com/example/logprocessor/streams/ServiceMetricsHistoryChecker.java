package com.example.logprocessor.streams;

import com.example.logprocessor.clickhouse.ClickHouseIdentifiers;
import com.example.logprocessor.config.ClickHouseProperties;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ServiceMetricsHistoryChecker {

  private final JdbcTemplate jdbcTemplate;
  private final String serviceMetricsTable;

  public ServiceMetricsHistoryChecker(
      JdbcTemplate jdbcTemplate, ClickHouseProperties properties) {
    this.jdbcTemplate = jdbcTemplate;
    this.serviceMetricsTable =
        ClickHouseIdentifiers.resolveTableName(properties.tableServiceMetrics(), "service_metrics");
  }

  /** True when we have rows old enough to cover a 7-day baseline window. */
  public boolean hasAtLeastSevenDays(String serviceName, String environment) {
    String sql =
        "SELECT min(window_start) FROM "
            + serviceMetricsTable
            + " WHERE service_name = ? AND environment = ?";
    var rows =
        jdbcTemplate.query(
            sql,
            (rs, rowNum) -> rs.getTimestamp(1),
            Objects.requireNonNullElse(serviceName, ""),
            Objects.requireNonNullElse(environment, ""));
    if (rows.isEmpty()) {
      return false;
    }
    java.sql.Timestamp min = rows.get(0);
    if (min == null) {
      return false;
    }
    long ageMs = System.currentTimeMillis() - min.getTime();
    return ageMs >= 7L * 24 * 60 * 60 * 1000;
  }
}
