package com.causr.llmrouter.anomaly;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AnomalyAlertEvent(
    UUID id,
    long windowStartEpochMs,
    long windowEndEpochMs,
    String tenantId,
    String serviceName,
    String environment,
    float anomalyScore,
    String featureJson) {}
