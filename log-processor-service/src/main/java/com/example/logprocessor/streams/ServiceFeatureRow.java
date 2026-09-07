package com.example.logprocessor.streams;

import java.time.LocalDateTime;

public record ServiceFeatureRow(
    LocalDateTime windowStartUtc,
    LocalDateTime windowEndUtc,
    String tenantId,
    String serviceName,
    String environment,
    long logVolume,
    long errorVolume,
    float errorRate,
    float p99LatencyMs,
    int uniqueErrorTypes,
    int newErrorTypes,
    byte silenceFlag,
    byte deploymentFlag,
    float timeOfDaySin,
    float timeOfDayCos,
    float aiAnomalyScore) {}
