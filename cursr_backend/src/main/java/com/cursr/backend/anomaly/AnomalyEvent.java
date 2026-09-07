package com.cursr.backend.anomaly;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AnomalyEvent(
    @JsonProperty("tenantId") String tenantId,
    @JsonProperty("service") String service,
    @JsonProperty("type") String type,
    @JsonProperty("message") String message,
    @JsonProperty("riskScore") Integer riskScore,
    @JsonProperty("timestamp") String timestamp,
    @JsonProperty("traceId") String traceId,
    @JsonProperty("clusterId") String clusterId) {

  public String resolvedTenantId() {
    return tenantId != null && !tenantId.isBlank() ? tenantId : "default";
  }
}
