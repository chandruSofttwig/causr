package com.example.logprocessor.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RawSpanEvent {

  public String trace_id;
  public String span_id;
  public String parent_span_id;
  public String tenant_id;
  public String service_name;
  public String environment;
  public String span_name;
  /** Short kind, e.g. SERVER, CLIENT, INTERNAL (empty if unspecified). */
  public String span_kind;
  /** UNSET, OK, or ERROR. */
  public String status_code;
  public long start_time_unix_nano;
  public int duration_ms;
  public Map<String, String> attributes;
}
