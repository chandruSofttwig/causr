package com.cursr.backend.config;

import com.clickhouse.jdbc.ClickHouseDataSource;
import java.util.Properties;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class ClickHouseConfig {

  @Bean
  public DataSource clickHouseDataSource(
    @Value("${clickhouse.url}") String url,
    @Value("${clickhouse.username}") String username,
    @Value("${clickhouse.password:}") String password
  ) throws Exception {
    Properties props = new Properties();
    props.setProperty("user", username);
    if (password != null && !password.isEmpty()) {
      props.setProperty("password", password);
    }
    return new ClickHouseDataSource(url, props);
  }

  @Bean
  public JdbcTemplate jdbcTemplate(DataSource dataSource) {
    return new JdbcTemplate(dataSource);
  }
}
