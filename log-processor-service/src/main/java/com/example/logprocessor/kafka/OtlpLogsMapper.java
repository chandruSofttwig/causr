package com.example.logprocessor.kafka;

import com.example.logprocessor.model.RawLogEvent;
import com.example.logprocessor.otlp.OtlpLogAttributeSupport;
import com.google.protobuf.ByteString;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.logs.v1.LogRecord;
import io.opentelemetry.proto.logs.v1.ResourceLogs;
import io.opentelemetry.proto.logs.v1.ScopeLogs;
import io.opentelemetry.proto.logs.v1.SeverityNumber;
import io.opentelemetry.proto.resource.v1.Resource;
import java.time.Instant;
import java.util.HexFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

/**
 * Maps OTLP {@link io.opentelemetry.proto.logs.v1.LogsData} resource/scope/log records to {@link RawLogEvent}.
 */
@Component
public class OtlpLogsMapper {

  private static final HexFormat LOWER_HEX = HexFormat.of().withLowerCase();

  private static final String ATTR_SERVICE_NAME = "service.name";
  private static final String ATTR_TENANT_ID = "tenant.id";
  private static final String ATTR_CLUSTER_ID = "log.cluster_id";
  private static final String ATTR_DEPLOYMENT_ENVIRONMENT = "deployment.environment";

  public void forEachRawEvent(
      ResourceLogs resourceLogs, Consumer<RawLogEvent> consumer) {
    Resource resource = resourceLogs.getResource();
    Map<String, String> resourceAttrs = attributesToStringMap(resource.getAttributesList());
    String serviceName = resourceAttrs.getOrDefault(ATTR_SERVICE_NAME, "unknown");
    String tenantId = resourceAttrs.getOrDefault(ATTR_TENANT_ID, "");

    for (ScopeLogs scopeLogs : resourceLogs.getScopeLogsList()) {
      for (LogRecord logRecord : scopeLogs.getLogRecordsList()) {
        RawLogEvent event = toRawLogEvent(serviceName, tenantId, resourceAttrs, logRecord);
        consumer.accept(event);
      }
    }
  }

  RawLogEvent toRawLogEvent(
      String serviceName,
      String tenantId,
      Map<String, String> resourceAttrs,
      LogRecord logRecord) {
    RawLogEvent e = new RawLogEvent();
    e.tenant_id = tenantId;
    e.log_level = severityText(logRecord);
    e.message = logRecord.hasBody() ? bodyToString(logRecord.getBody()) : "";
    e.timestamp = formatTimeUnixNano(logRecord.getTimeUnixNano());
    e.trace_id = traceIdToHex(logRecord.getTraceId());
    e.span_id = spanIdToHex(logRecord.getSpanId());
    e.anomaly_score = null;

    Map<String, String> attrs = new HashMap<>(resourceAttrs);
    putStringAttributes(attrs, logRecord.getAttributesList());
    OtlpLogAttributeSupport.normalizeHttpStatusCodeAlias(attrs);

    e.service_name = firstNonBlank(attrs.get(ATTR_SERVICE_NAME), serviceName, "unknown");
    e.environment =
        firstNonBlank(attrs.get(ATTR_DEPLOYMENT_ENVIRONMENT), resourceAttrs.get(ATTR_DEPLOYMENT_ENVIRONMENT), "unknown");
    e.duration_ms =
        OtlpLogAttributeSupport.toNullableDurationMsInt(
            OtlpLogAttributeSupport.latencyMsFromAttributes(attrs));
    e.attributes = attrs;

    e.cluster_id = attrs.getOrDefault(ATTR_CLUSTER_ID, "");

    return e;
  }

  private static String severityText(LogRecord logRecord) {
    String text = logRecord.getSeverityText();
    if (text != null && !text.isBlank()) {
      return text.trim().toUpperCase();
    }
    SeverityNumber sn = logRecord.getSeverityNumber();
    if (sn == null || sn == SeverityNumber.SEVERITY_NUMBER_UNSPECIFIED || sn == SeverityNumber.UNRECOGNIZED) {
      return "";
    }
    return severityNumberToLevel(sn);
  }

  /**
   * Maps OTLP SeverityNumber ranges to short levels (avoids SEVERITY_NUMBER_* enum names).
   *
   * @see <a href="https://opentelemetry.io/docs/specs/otel/logs/data-model/#displaying-severity">OTLP severity</a>
   */
  static String severityNumberToLevel(SeverityNumber sn) {
    int n = sn.getNumber();
    if (n >= 21) {
      return "FATAL";
    }
    if (n >= 17) {
      return "ERROR";
    }
    if (n >= 13) {
      return "WARN";
    }
    if (n >= 9) {
      return "INFO";
    }
    if (n >= 5) {
      return "DEBUG";
    }
    if (n >= 1) {
      return "TRACE";
    }
    return "";
  }

  private static String formatTimeUnixNano(long timeUnixNano) {
    if (timeUnixNano <= 0) {
      return Instant.now().toString();
    }
    long seconds = timeUnixNano / 1_000_000_000L;
    long nanos = timeUnixNano % 1_000_000_000L;
    return Instant.ofEpochSecond(seconds, nanos).toString();
  }

  private static String traceIdToHex(ByteString bytes) {
    return idBytesToHex(bytes, 16);
  }

  private static String spanIdToHex(ByteString bytes) {
    return idBytesToHex(bytes, 8);
  }

  private static String idBytesToHex(ByteString bytes, int expectedLen) {
    if (bytes == null || bytes.isEmpty()) {
      return "";
    }
    byte[] arr = bytes.toByteArray();
    if (arr.length != expectedLen || isAllZero(arr)) {
      return "";
    }
    return LOWER_HEX.formatHex(arr);
  }

  private static boolean isAllZero(byte[] arr) {
    for (byte b : arr) {
      if (b != 0) {
        return false;
      }
    }
    return true;
  }

  private static String bodyToString(AnyValue body) {
    if (body == null || body.equals(AnyValue.getDefaultInstance())) {
      return "";
    }
    if (body.hasStringValue()) {
      return body.getStringValue();
    }
    if (body.hasBoolValue()) {
      return Boolean.toString(body.getBoolValue());
    }
    if (body.hasIntValue()) {
      return Long.toString(body.getIntValue());
    }
    if (body.hasDoubleValue()) {
      return Double.toString(body.getDoubleValue());
    }
    if (body.hasArrayValue()) {
      List<String> parts = new ArrayList<>();
      for (AnyValue v : body.getArrayValue().getValuesList()) {
        parts.add(bodyToString(v));
      }
      return String.join(",", parts);
    }
    if (body.hasKvlistValue()) {
      StringBuilder sb = new StringBuilder();
      for (KeyValue kv : body.getKvlistValue().getValuesList()) {
        if (!sb.isEmpty()) {
          sb.append(' ');
        }
        sb.append(kv.getKey()).append('=').append(anyValueToString(kv.getValue()));
      }
      return sb.toString();
    }
    if (body.hasBytesValue()) {
      return body.getBytesValue().toStringUtf8();
    }
    return "";
  }

  private static String anyValueToString(AnyValue v) {
    return bodyToString(v);
  }

  private static Map<String, String> attributesToStringMap(List<KeyValue> keyValues) {
    Map<String, String> map = new HashMap<>();
    putStringAttributes(map, keyValues);
    return map;
  }

  private static void putStringAttributes(Map<String, String> target, List<KeyValue> keyValues) {
    for (KeyValue kv : keyValues) {
      String s = anyValueToString(kv.getValue());
      if (s != null && !s.isEmpty()) {
        target.put(kv.getKey(), s);
      }
    }
  }

  private static String firstNonBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value;
      }
    }
    return "";
  }
}
