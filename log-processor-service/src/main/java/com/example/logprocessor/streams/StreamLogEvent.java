package com.example.logprocessor.streams;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record StreamLogEvent(
    String tenantId,
    String serviceName,
    String environment,
    boolean error,
    long latencyMs,
    String templateHash) {}
