package com.example.logprocessor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.traces")
public record TracesIngestProperties(
    /**
     * When true, only {@code SERVER} spans are written to ClickHouse (reduces volume; dashboard
     * latency/RPM use SERVER spans only).
     */
    boolean serverSpansOnly) {}
