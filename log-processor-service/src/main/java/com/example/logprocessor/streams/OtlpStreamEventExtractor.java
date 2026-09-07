package com.example.logprocessor.streams;

import com.example.logprocessor.otlp.OtlpLogAttributeSupport;
import com.google.protobuf.InvalidProtocolBufferException;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.logs.v1.LogRecord;
import io.opentelemetry.proto.logs.v1.ResourceLogs;
import io.opentelemetry.proto.logs.v1.ScopeLogs;
import io.opentelemetry.proto.logs.v1.SeverityNumber;
import io.opentelemetry.proto.resource.v1.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.kafka.streams.KeyValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Parses OTLP export payloads into {@link StreamLogEvent} rows for aggregation. */
public final class OtlpStreamEventExtractor {

  private static final Logger log = LoggerFactory.getLogger(OtlpStreamEventExtractor.class);

  private static final String ATTR_SERVICE_NAME = "service.name";
  private static final String ATTR_TENANT_ID = "tenant.id";
  private static final String ATTR_DEPLOYMENT_ENVIRONMENT = "deployment.environment";
  private static final String ATTR_CLUSTER_ID = "log.cluster_id";

  private OtlpStreamEventExtractor() {}

  public static List<KeyValue<String, StreamLogEvent>> toKeyedEvents(byte[] value) {
    List<KeyValue<String, StreamLogEvent>> out = new ArrayList<>();
    if (value == null || value.length == 0) {
      return out;
    }
    final ExportLogsServiceRequest request;
    try {
      request = ExportLogsServiceRequest.parseFrom(value);
    } catch (InvalidProtocolBufferException e) {
      log.debug("Streams parse skip: {}", e.getMessage());
      return out;
    }
    for (ResourceLogs rl : request.getResourceLogsList()) {
      Resource resource = rl.getResource();
      Map<String, String> attrs = attributesToStringMap(resource.getAttributesList());
      String service = attrs.getOrDefault(ATTR_SERVICE_NAME, "unknown");
      String tenant = attrs.getOrDefault(ATTR_TENANT_ID, "");
      String environment = attrs.getOrDefault(ATTR_DEPLOYMENT_ENVIRONMENT, "unknown");
      for (ScopeLogs sl : rl.getScopeLogsList()) {
        for (LogRecord lr : sl.getLogRecordsList()) {
          StreamLogEvent ev = toEvent(tenant, service, environment, attrs, lr);
          String groupKey = service + "#" + environment;
          out.add(KeyValue.pair(groupKey, ev));
        }
      }
    }
    return out;
  }

  private static StreamLogEvent toEvent(
      String tenant,
      String service,
      String environment,
      Map<String, String> resourceAttrs,
      LogRecord lr) {
    Map<String, String> map = new HashMap<>(resourceAttrs);
    putStringAttributes(map, lr.getAttributesList());
    OtlpLogAttributeSupport.normalizeHttpStatusCodeAlias(map);
    boolean error = isError(lr, map);
    long latencyMs = OtlpLogAttributeSupport.latencyMsFromAttributes(map);
    String templateHash = templateHash(map, lr);
    return new StreamLogEvent(tenant, service, environment, error, latencyMs, templateHash);
  }

  private static boolean isError(LogRecord lr, Map<String, String> attrs) {
    String text = lr.getSeverityText();
    if (text != null && text.toUpperCase().contains("ERROR")) {
      return true;
    }
    SeverityNumber sn = lr.getSeverityNumber();
    if (sn != null
        && sn != SeverityNumber.SEVERITY_NUMBER_UNSPECIFIED
        && sn != SeverityNumber.UNRECOGNIZED) {
      int n = sn.getNumber();
      if (n >= 17) {
        return true;
      }
    }
    String level = attrs.get("log.level");
    if (level != null && level.toUpperCase().contains("ERROR")) {
      return true;
    }
    return false;
  }

  private static String templateHash(Map<String, String> attrs, LogRecord lr) {
    String cid = attrs.get(ATTR_CLUSTER_ID);
    if (cid != null && !cid.isBlank()) {
      return cid;
    }
    String msg = bodyToString(lr);
    return String.valueOf(java.util.Objects.hash(msg));
  }

  private static String bodyToString(LogRecord lr) {
    if (!lr.hasBody()) {
      return "";
    }
    AnyValue body = lr.getBody();
    if (body.hasStringValue()) {
      return body.getStringValue();
    }
    return body.toString();
  }

  private static Map<String, String> attributesToStringMap(
      List<io.opentelemetry.proto.common.v1.KeyValue> keyValues) {
    Map<String, String> map = new HashMap<>();
    putStringAttributes(map, keyValues);
    return map;
  }

  private static void putStringAttributes(
      Map<String, String> target,
      List<io.opentelemetry.proto.common.v1.KeyValue> keyValues) {
    for (io.opentelemetry.proto.common.v1.KeyValue kv : keyValues) {
      String s = anyValueToString(kv.getValue());
      if (s != null && !s.isEmpty()) {
        target.put(kv.getKey(), s);
      }
    }
  }

  private static String anyValueToString(AnyValue v) {
    if (v == null || v.equals(AnyValue.getDefaultInstance())) {
      return "";
    }
    if (v.hasStringValue()) {
      return v.getStringValue();
    }
    if (v.hasBoolValue()) {
      return Boolean.toString(v.getBoolValue());
    }
    if (v.hasIntValue()) {
      return Long.toString(v.getIntValue());
    }
    if (v.hasDoubleValue()) {
      return Double.toString(v.getDoubleValue());
    }
    return "";
  }
}
