package com.example.logprocessor.kafka;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.logprocessor.model.RawSpanEvent;
import com.google.protobuf.ByteString;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.resource.v1.Resource;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.ScopeSpans;
import io.opentelemetry.proto.trace.v1.Span;
import io.opentelemetry.proto.trace.v1.Status;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class OtlpTracesMapperTest {

  @Test
  void mapsSpanIdsKindDurationStatusAndHttpAlias() {
    byte[] traceBytes = new byte[16];
    for (int i = 0; i < 16; i++) {
      traceBytes[i] = (byte) (i + 1);
    }
    byte[] spanBytes = new byte[] {1, 2, 3, 4, 5, 6, 7, 8};
    byte[] parentBytes = new byte[] {8, 7, 6, 5, 4, 3, 2, 1};

    long startNs = 1_700_000_000_000_000_000L;
    long endNs = startNs + 2_500_000L;

    ResourceSpans rs =
        ResourceSpans.newBuilder()
            .setResource(
                Resource.newBuilder()
                    .addAttributes(
                        KeyValue.newBuilder()
                            .setKey("service.name")
                            .setValue(AnyValue.newBuilder().setStringValue("orders-api").build())
                            .build())
                    .addAttributes(
                        KeyValue.newBuilder()
                            .setKey("tenant.id")
                            .setValue(AnyValue.newBuilder().setStringValue("t1").build())
                            .build())
                    .addAttributes(
                        KeyValue.newBuilder()
                            .setKey("deployment.environment")
                            .setValue(AnyValue.newBuilder().setStringValue("prod").build())
                            .build())
                    .build())
            .addScopeSpans(
                ScopeSpans.newBuilder()
                    .addSpans(
                        Span.newBuilder()
                            .setTraceId(ByteString.copyFrom(traceBytes))
                            .setSpanId(ByteString.copyFrom(spanBytes))
                            .setParentSpanId(ByteString.copyFrom(parentBytes))
                            .setName("GET /orders")
                            .setKind(Span.SpanKind.SPAN_KIND_SERVER)
                            .setStartTimeUnixNano(startNs)
                            .setEndTimeUnixNano(endNs)
                            .setStatus(
                                Status.newBuilder()
                                    .setCode(Status.StatusCode.STATUS_CODE_ERROR)
                                    .build())
                            .addAttributes(
                                KeyValue.newBuilder()
                                    .setKey("http.response.status_code")
                                    .setValue(AnyValue.newBuilder().setIntValue(503).build())
                                    .build())
                            .build())
                    .build())
            .build();

    OtlpTracesMapper mapper = new OtlpTracesMapper();
    List<RawSpanEvent> out = new ArrayList<>();
    mapper.forEachRawSpan(rs, out::add);

    assertEquals(1, out.size());
    RawSpanEvent e = out.get(0);
    assertEquals("orders-api", e.service_name);
    assertEquals("t1", e.tenant_id);
    assertEquals("prod", e.environment);
    assertEquals("0102030405060708090a0b0c0d0e0f10", e.trace_id);
    assertEquals("0102030405060708", e.span_id);
    assertEquals("0807060504030201", e.parent_span_id);
    assertEquals("GET /orders", e.span_name);
    assertEquals("SERVER", e.span_kind);
    assertEquals("ERROR", e.status_code);
    assertEquals(2, e.duration_ms);
    assertEquals("503", e.attributes.get("http.status_code"));
    assertEquals("503", e.attributes.get("http.response.status_code"));
  }
}
