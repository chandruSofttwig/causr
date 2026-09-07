package com.example.logprocessor.kafka;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.example.logprocessor.model.RawLogEvent;
import com.google.protobuf.ByteString;
import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.logs.v1.LogRecord;
import io.opentelemetry.proto.logs.v1.LogsData;
import io.opentelemetry.proto.logs.v1.ResourceLogs;
import io.opentelemetry.proto.logs.v1.ScopeLogs;
import io.opentelemetry.proto.resource.v1.Resource;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class OtlpLogsMapperTest {

  @Test
  void mapsServiceTenantBodyTraceSpanAndCluster() throws Exception {
    byte[] traceBytes = new byte[16];
    for (int i = 0; i < 16; i++) {
      traceBytes[i] = (byte) (i + 1);
    }
    byte[] spanBytes = new byte[] {1, 2, 3, 4, 5, 6, 7, 8};

    LogsData data =
        LogsData.newBuilder()
            .addResourceLogs(
                ResourceLogs.newBuilder()
                    .setResource(
                        Resource.newBuilder()
                            .addAttributes(
                                KeyValue.newBuilder()
                                    .setKey("service.name")
                                    .setValue(
                                        AnyValue.newBuilder().setStringValue("payments-api").build())
                                    .build())
                            .addAttributes(
                                KeyValue.newBuilder()
                                    .setKey("tenant.id")
                                    .setValue(AnyValue.newBuilder().setStringValue("tenant-1").build())
                                    .build())
                            .build())
                    .addScopeLogs(
                        ScopeLogs.newBuilder()
                            .addLogRecords(
                                LogRecord.newBuilder()
                                    .setTimeUnixNano(1_700_000_000_000_000_000L)
                                    .setSeverityText("ERROR")
                                    .setBody(AnyValue.newBuilder().setStringValue("timeout").build())
                                    .setTraceId(ByteString.copyFrom(traceBytes))
                                    .setSpanId(ByteString.copyFrom(spanBytes))
                                    .addAttributes(
                                        KeyValue.newBuilder()
                                            .setKey("log.cluster_id")
                                            .setValue(AnyValue.newBuilder().setStringValue("err_t").build())
                                            .build())
                                    .build())
                            .build())
                    .build())
            .build();

    assertEquals(data, LogsData.parseFrom(data.toByteArray()));

    OtlpLogsMapper mapper = new OtlpLogsMapper();
    List<RawLogEvent> events = new ArrayList<>();
    mapper.forEachRawEvent(data.getResourceLogs(0), events::add);

    assertEquals(1, events.size());
    RawLogEvent e = events.get(0);
    assertEquals("payments-api", e.service_name);
    assertEquals("unknown", e.environment);
    assertEquals("tenant-1", e.tenant_id);
    assertEquals("ERROR", e.log_level);
    assertEquals("timeout", e.message);
    assertEquals(Instant.ofEpochSecond(1_700_000_000L, 0).toString(), e.timestamp);
    assertEquals("0102030405060708090a0b0c0d0e0f10", e.trace_id);
    assertEquals("0102030405060708", e.span_id);
    assertEquals("err_t", e.cluster_id);
    assertEquals("payments-api", e.attributes.get("service.name"));
    assertEquals("err_t", e.attributes.get("log.cluster_id"));
  }

  @Test
  void mapsDurationMsFromLogAttribute() {
    LogsData data =
        LogsData.newBuilder()
            .addResourceLogs(
                ResourceLogs.newBuilder()
                    .setResource(
                        Resource.newBuilder()
                            .addAttributes(
                                KeyValue.newBuilder()
                                    .setKey("service.name")
                                    .setValue(AnyValue.newBuilder().setStringValue("api").build())
                                    .build())
                            .build())
                    .addScopeLogs(
                        ScopeLogs.newBuilder()
                            .addLogRecords(
                                LogRecord.newBuilder()
                                    .setTimeUnixNano(1_000_000_000L)
                                    .setBody(AnyValue.newBuilder().setStringValue("ok").build())
                                    .addAttributes(
                                        KeyValue.newBuilder()
                                            .setKey("duration_ms")
                                            .setValue(AnyValue.newBuilder().setIntValue(250).build())
                                            .build())
                                    .build())
                            .build())
                    .build())
            .build();

    OtlpLogsMapper mapper = new OtlpLogsMapper();
    List<RawLogEvent> events = new ArrayList<>();
    mapper.forEachRawEvent(data.getResourceLogs(0), events::add);

    assertEquals(250, events.get(0).duration_ms);
  }

  @Test
  void mapsHttpResponseStatusCodeToLegacyKeyForDashboardSql() {
    LogsData data =
        LogsData.newBuilder()
            .addResourceLogs(
                ResourceLogs.newBuilder()
                    .setResource(Resource.newBuilder().build())
                    .addScopeLogs(
                        ScopeLogs.newBuilder()
                            .addLogRecords(
                                LogRecord.newBuilder()
                                    .setTimeUnixNano(1_000_000_000L)
                                    .setBody(AnyValue.newBuilder().setStringValue("x").build())
                                    .addAttributes(
                                        KeyValue.newBuilder()
                                            .setKey("http.response.status_code")
                                            .setValue(AnyValue.newBuilder().setIntValue(502).build())
                                            .build())
                                    .build())
                            .build())
                    .build())
            .build();

    OtlpLogsMapper mapper = new OtlpLogsMapper();
    List<RawLogEvent> events = new ArrayList<>();
    mapper.forEachRawEvent(data.getResourceLogs(0), events::add);

    assertEquals("502", events.get(0).attributes.get("http.status_code"));
    assertEquals("502", events.get(0).attributes.get("http.response.status_code"));
  }

  @Test
  void leavesDurationMsNullWhenNoLatencyAttributes() {
    LogsData data =
        LogsData.newBuilder()
            .addResourceLogs(
                ResourceLogs.newBuilder()
                    .setResource(Resource.newBuilder().build())
                    .addScopeLogs(
                        ScopeLogs.newBuilder()
                            .addLogRecords(
                                LogRecord.newBuilder()
                                    .setTimeUnixNano(1_000_000_000L)
                                    .setBody(AnyValue.newBuilder().setStringValue("x").build())
                                    .build())
                            .build())
                    .build())
            .build();

    OtlpLogsMapper mapper = new OtlpLogsMapper();
    List<RawLogEvent> events = new ArrayList<>();
    mapper.forEachRawEvent(data.getResourceLogs(0), events::add);

    assertNull(events.get(0).duration_ms);
  }

  @Test
  void logRecordServiceNameOverridesResource() {
    LogsData data =
        LogsData.newBuilder()
            .addResourceLogs(
                ResourceLogs.newBuilder()
                    .setResource(
                        Resource.newBuilder()
                            .addAttributes(
                                KeyValue.newBuilder()
                                    .setKey("service.name")
                                    .setValue(
                                        AnyValue.newBuilder().setStringValue("log-sender-backend").build())
                                    .build())
                            .build())
                    .addScopeLogs(
                        ScopeLogs.newBuilder()
                            .addLogRecords(
                                LogRecord.newBuilder()
                                    .setTimeUnixNano(1_000_000_000L)
                                    .setBody(AnyValue.newBuilder().setStringValue("err").build())
                                    .addAttributes(
                                        KeyValue.newBuilder()
                                            .setKey("service.name")
                                            .setValue(
                                                AnyValue.newBuilder().setStringValue("payment-service").build())
                                            .build())
                                    .build())
                            .build())
                    .build())
            .build();

    OtlpLogsMapper mapper = new OtlpLogsMapper();
    List<RawLogEvent> events = new ArrayList<>();
    mapper.forEachRawEvent(data.getResourceLogs(0), events::add);

    assertEquals("payment-service", events.get(0).service_name);
  }

  @Test
  void mapsAppLatencyMsFromLogAttribute() {
    LogsData data =
        LogsData.newBuilder()
            .addResourceLogs(
                ResourceLogs.newBuilder()
                    .setResource(Resource.newBuilder().build())
                    .addScopeLogs(
                        ScopeLogs.newBuilder()
                            .addLogRecords(
                                LogRecord.newBuilder()
                                    .setTimeUnixNano(1_000_000_000L)
                                    .setBody(AnyValue.newBuilder().setStringValue("slow").build())
                                    .addAttributes(
                                        KeyValue.newBuilder()
                                            .setKey("app.latency_ms")
                                            .setValue(AnyValue.newBuilder().setIntValue(1176).build())
                                            .build())
                                    .build())
                            .build())
                    .build())
            .build();

    OtlpLogsMapper mapper = new OtlpLogsMapper();
    List<RawLogEvent> events = new ArrayList<>();
    mapper.forEachRawEvent(data.getResourceLogs(0), events::add);

    assertEquals(1176, events.get(0).duration_ms);
  }

  @Test
  void logRecordEnvironmentOverridesResource() {
    LogsData data =
        LogsData.newBuilder()
            .addResourceLogs(
                ResourceLogs.newBuilder()
                    .setResource(
                        Resource.newBuilder()
                            .addAttributes(
                                KeyValue.newBuilder()
                                    .setKey("deployment.environment")
                                    .setValue(AnyValue.newBuilder().setStringValue("prod").build())
                                    .build())
                            .build())
                    .addScopeLogs(
                        ScopeLogs.newBuilder()
                            .addLogRecords(
                                LogRecord.newBuilder()
                                    .setTimeUnixNano(1_000_000_000L)
                                    .setBody(AnyValue.newBuilder().setStringValue("x").build())
                                    .addAttributes(
                                        KeyValue.newBuilder()
                                            .setKey("deployment.environment")
                                            .setValue(AnyValue.newBuilder().setStringValue("staging").build())
                                            .build())
                                    .build())
                            .build())
                    .build())
            .build();

    OtlpLogsMapper mapper = new OtlpLogsMapper();
    List<RawLogEvent> events = new ArrayList<>();
    mapper.forEachRawEvent(data.getResourceLogs(0), events::add);

    assertEquals("staging", events.get(0).environment);
  }

  @Test
  void mapsSeverityNumberToShortLevelWhenTextMissing() {
    LogsData data =
        LogsData.newBuilder()
            .addResourceLogs(
                ResourceLogs.newBuilder()
                    .setResource(Resource.newBuilder().build())
                    .addScopeLogs(
                        ScopeLogs.newBuilder()
                            .addLogRecords(
                                LogRecord.newBuilder()
                                    .setTimeUnixNano(1_000_000_000L)
                                    .setSeverityNumber(
                                        io.opentelemetry.proto.logs.v1.SeverityNumber.SEVERITY_NUMBER_WARN)
                                    .setBody(AnyValue.newBuilder().setStringValue("warn").build())
                                    .build())
                            .build())
                    .build())
            .build();

    OtlpLogsMapper mapper = new OtlpLogsMapper();
    List<RawLogEvent> events = new ArrayList<>();
    mapper.forEachRawEvent(data.getResourceLogs(0), events::add);

    assertEquals("WARN", events.get(0).log_level);
  }

  @Test
  void severityNumberToLevelCoversRanges() {
    assertEquals(
        "ERROR",
        OtlpLogsMapper.severityNumberToLevel(
            io.opentelemetry.proto.logs.v1.SeverityNumber.SEVERITY_NUMBER_ERROR));
    assertEquals(
        "INFO",
        OtlpLogsMapper.severityNumberToLevel(
            io.opentelemetry.proto.logs.v1.SeverityNumber.SEVERITY_NUMBER_INFO));
    assertEquals(
        "DEBUG",
        OtlpLogsMapper.severityNumberToLevel(
            io.opentelemetry.proto.logs.v1.SeverityNumber.SEVERITY_NUMBER_DEBUG));
    assertEquals(
        "TRACE",
        OtlpLogsMapper.severityNumberToLevel(
            io.opentelemetry.proto.logs.v1.SeverityNumber.SEVERITY_NUMBER_TRACE));
    assertEquals(
        "FATAL",
        OtlpLogsMapper.severityNumberToLevel(
            io.opentelemetry.proto.logs.v1.SeverityNumber.SEVERITY_NUMBER_FATAL));
  }

  @Test
  void allZeroTraceAndSpanMapToEmptyStrings() {
    LogsData data =
        LogsData.newBuilder()
            .addResourceLogs(
                ResourceLogs.newBuilder()
                    .setResource(Resource.newBuilder().build())
                    .addScopeLogs(
                        ScopeLogs.newBuilder()
                            .addLogRecords(
                                LogRecord.newBuilder()
                                    .setTimeUnixNano(1_000_000_000L)
                                    .setTraceId(ByteString.copyFrom(new byte[16]))
                                    .setSpanId(ByteString.copyFrom(new byte[8]))
                                    .setBody(AnyValue.newBuilder().setStringValue("x").build())
                                    .build())
                            .build())
                    .build())
            .build();

    OtlpLogsMapper mapper = new OtlpLogsMapper();
    List<RawLogEvent> events = new ArrayList<>();
    mapper.forEachRawEvent(data.getResourceLogs(0), events::add);

    assertEquals("", events.get(0).trace_id);
    assertEquals("", events.get(0).span_id);
  }
}
