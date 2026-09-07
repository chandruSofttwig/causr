package com.cursr.backend.anomaly;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

/** Published by llm-router-service on topic rca-ready after Claude fills rca_text. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RcaReadyEvent(
    String type,
    UUID id,
    long windowStartEpochMs,
    long windowEndEpochMs,
    String tenantId,
    String serviceName,
    String environment,
    float anomalyScore,
    String featureJson,
    String rcaText,
    String rcaGeneratedAt) {}
