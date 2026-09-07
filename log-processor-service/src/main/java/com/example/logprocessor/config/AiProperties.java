package com.example.logprocessor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai")
public record AiProperties(
    String grpcTarget,
    String httpBaseUrl,
    Boolean enableClustering,
    Boolean bypassHistoryGate,
    Float anomalyThresholdOverride,
    Boolean forcePublishAnomaly,
    Boolean devEndpointsEnabled) {

  public AiProperties {
    if (grpcTarget == null || grpcTarget.isBlank()) {
      grpcTarget = "localhost:50051";
    }
    if (httpBaseUrl == null || httpBaseUrl.isBlank()) {
      httpBaseUrl = "http://localhost:8000";
    }
    // When set, we treat ai_score < threshold as anomaly (dev override).
    if (anomalyThresholdOverride != null && !Float.isFinite(anomalyThresholdOverride)) {
      anomalyThresholdOverride = null;
    }
  }

  public boolean clusteringEnabled() {
    return enableClustering == null || enableClustering;
  }

  public boolean bypassHistoryGateOrDefault() {
    return bypassHistoryGate != null && bypassHistoryGate;
  }

  public boolean forcePublishAnomalyOrDefault() {
    return forcePublishAnomaly != null && forcePublishAnomaly;
  }

  public boolean devEndpointsEnabledOrDefault() {
    return devEndpointsEnabled != null && devEndpointsEnabled;
  }
}
