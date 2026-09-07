package com.example.logprocessor.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RawLogEvent {

  public String timestamp;
  public String tenant_id;
  public String service_name;
  /** deployment.environment resource attribute, default unknown */
  public String environment;
  public String log_level;
  public String message;
  public String trace_id;
  public String span_id;
  public Integer duration_ms;
  public Float anomaly_score;
  public String cluster_id;
  public Map<String, String> attributes;
}
