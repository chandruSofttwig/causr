package com.causr.llmrouter.anomaly;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
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
    String rcaGeneratedAt) {

  public static RcaReadyEvent from(AnomalyAlertEvent anomaly, String rcaText, String rcaGeneratedAt) {
    return new RcaReadyEvent(
        "rca-ready",
        anomaly.id(),
        anomaly.windowStartEpochMs(),
        anomaly.windowEndEpochMs(),
        anomaly.tenantId(),
        anomaly.serviceName(),
        anomaly.environment(),
        anomaly.anomalyScore(),
        anomaly.featureJson(),
        rcaText,
        rcaGeneratedAt);
  }
}
