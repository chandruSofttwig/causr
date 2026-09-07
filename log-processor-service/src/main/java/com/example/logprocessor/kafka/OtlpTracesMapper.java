package com.example.logprocessor.kafka;

import com.example.logprocessor.model.RawSpanEvent;
import com.example.logprocessor.otlp.OtlpLogAttributeSupport;
import com.google.protobuf.ByteString;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.resource.v1.Resource;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import io.opentelemetry.proto.trace.v1.Span;
import io.opentelemetry.proto.trace.v1.Status;
import java.util.HexFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class OtlpTracesMapper {

  private static final HexFormat LOWER_HEX = HexFormat.of().withLowerCase();

  private static final String ATTR_SERVICE_NAME = "service.name";
  private static final String ATTR_TENANT_ID = "tenant.id";
  private static final String ATTR_DEPLOYMENT_ENVIRONMENT = "deployment.environment";

  public void forEachRawSpan(ResourceSpans resourceSpans, Consumer<RawSpanEvent> consumer) {
    Resource resource = resourceSpans.getResource();
    Map<String, String> resourceAttrs = attributesToStringMap(resource.getAttributesList());
    String serviceName = resourceAttrs.getOrDefault(ATTR_SERVICE_NAME, "unknown");
    String tenantId = resourceAttrs.getOrDefault(ATTR_TENANT_ID, "");
    String environment = resourceAttrs.getOrDefault(ATTR_DEPLOYMENT_ENVIRONMENT, "unknown");

    for (ScopeSpans scopeSpans : resourceSpans.getScopeSpansList()) {
      for (Span span : scopeSpans.getSpansList()) {
        RawSpanEvent e = toRawSpan(tenantId, serviceName, environment, resourceAttrs, span);
        consumer.accept(e);
      }
    }
  }

  RawSpanEvent toRawSpan(
      String tenantId,
      String serviceName,
      String environment,
      Map<String, String> resourceAttrs,
      Span span) {
    RawSpanEvent e = new RawSpanEvent();
    e.tenant_id = tenantId;
    e.service_name = serviceName;
    e.environment = environment;
    e.trace_id = traceIdToHex(span.getTraceId());
    e.span_id = spanIdToHex(span.getSpanId());
    e.parent_span_id = parentSpanIdToHex(span.getParentSpanId());
    e.span_name = span.getName();
    e.span_kind = kindShortName(span.getKind());
    e.status_code = statusCodeName(span);
    e.start_time_unix_nano = span.getStartTimeUnixNano();
    e.duration_ms = durationMs(span.getStartTimeUnixNano(), span.getEndTimeUnixNano());

    Map<String, String> attrs = new HashMap<>(resourceAttrs);
    putStringAttributes(attrs, span.getAttributesList());
    OtlpLogAttributeSupport.normalizeHttpStatusCodeAlias(attrs);
    e.attributes = attrs;

    return e;
  }

  private static String statusCodeName(Span span) {
    if (!span.hasStatus()) {
      return "UNSET";
    }
    Status status = span.getStatus();
    Status.StatusCode code = status.getCode();
    if (code == null || code == Status.StatusCode.UNRECOGNIZED) {
      return "UNSET";
    }
    return switch (code) {
      case STATUS_CODE_UNSET -> "UNSET";
      case STATUS_CODE_OK -> "OK";
      case STATUS_CODE_ERROR -> "ERROR";
      default -> "UNSET";
    };
  }

  private static String kindShortName(Span.SpanKind kind) {
    if (kind == null
        || kind == Span.SpanKind.UNRECOGNIZED
        || kind == Span.SpanKind.SPAN_KIND_UNSPECIFIED) {
      return "";
    }
    String n = kind.name();
    return n.startsWith("SPAN_KIND_") ? n.substring("SPAN_KIND_".length()) : n;
  }

  private static int durationMs(long startUnixNano, long endUnixNano) {
    if (startUnixNano <= 0 || endUnixNano <= 0 || endUnixNano < startUnixNano) {
      return 0;
    }
    long deltaNs = endUnixNano - startUnixNano;
    long ms = deltaNs / 1_000_000L;
    if (ms > 86_400_000L) {
      return 86_400_000;
    }
    return (int) Math.max(0L, ms);
  }

  private static String traceIdToHex(ByteString bytes) {
    return idBytesToHex(bytes, 16);
  }

  private static String spanIdToHex(ByteString bytes) {
    return idBytesToHex(bytes, 8);
  }

  private static String parentSpanIdToHex(ByteString bytes) {
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
