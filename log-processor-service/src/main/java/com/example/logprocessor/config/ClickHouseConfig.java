package com.example.logprocessor.config;

import com.clickhouse.jdbc.ClickHouseDataSource;
import java.sql.SQLException;
import java.util.Properties;
import javax.sql.DataSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@EnableConfigurationProperties({
  ClickHouseProperties.class,
  AiProperties.class,
  StreamsAppProperties.class,
  TracesIngestProperties.class
})
public class ClickHouseConfig {

  @Bean
  public DataSource clickHouseDataSource(ClickHouseProperties properties) throws SQLException {
    Properties jdbcProperties = new Properties();
    if (properties.username() != null) {
      jdbcProperties.setProperty("user", properties.username());
    }
    if (properties.password() != null) {
      jdbcProperties.setProperty("password", properties.password());
    }
    if (hasText(properties.serverTimeZone()) && hasText(properties.serverVersion())) {
      jdbcProperties.setProperty("server_time_zone", properties.serverTimeZone());
      jdbcProperties.setProperty("server_version", properties.serverVersion());
    }
    return new ClickHouseDataSource(properties.url(), jdbcProperties);
  }

  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  @Bean
  public JdbcTemplate jdbcTemplate(DataSource dataSource) {
    return new JdbcTemplate(dataSource);
  }
}
