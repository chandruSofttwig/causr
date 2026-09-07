package com.cursr.backend.anomaly;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

/** Mirrors log-processor {@code AnomalyKafkaMessage} JSON on topic anomaly-alerts. */
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
