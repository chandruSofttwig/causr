package com.example.logprocessor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "clickhouse")
public record ClickHouseProperties(
    String url,
    String username,
    String password,
    /**
     * Hot-tier table (ERROR/WARN, slow, keywords, anomaly).
     */
    String tableLogsHot,
    /**
     * Cold-tier table (random sample of other logs).
     */
    String tableLogsCold,
    /**
     * 30s aggregate feature rows per service/environment.
     */
    String tableServiceMetrics,
    /**
     * Detected anomaly events (AI score + RCA).
     */
    String tableAnomalies,
    /**
     * OTLP trace spans (latency / trace drill-down).
     */
    String tableSpans,
    /**
     * When set together with {@code serverVersion}, passed to the JDBC driver as {@code server_time_zone}
     * so the driver skips the startup query that calls {@code currentUser()} (which fails on some
     * endpoints or older servers).
     */
    String serverTimeZone,
    /**
     * Same as {@link #serverTimeZone}; use the value from {@code SELECT version()} on your server when
     * possible.
     */
    String serverVersion
) {}
