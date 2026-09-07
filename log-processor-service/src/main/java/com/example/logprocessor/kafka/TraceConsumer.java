package com.example.logprocessor.kafka;

import com.example.logprocessor.clickhouse.ClickHouseService;
import com.example.logprocessor.config.TracesIngestProperties;
import com.example.logprocessor.metrics.LogProcessingMetrics;
import com.example.logprocessor.model.RawSpanEvent;
import com.google.protobuf.InvalidProtocolBufferException;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class TraceConsumer {

  private static final Logger log = LoggerFactory.getLogger(TraceConsumer.class);

  private final OtlpTracesMapper otlpTracesMapper;
  private final ClickHouseService clickHouseService;
  private final LogProcessingMetrics metrics;
  private final TracesIngestProperties tracesIngestProperties;

  public TraceConsumer(
      OtlpTracesMapper otlpTracesMapper,
      ClickHouseService clickHouseService,
      LogProcessingMetrics metrics,
      TracesIngestProperties tracesIngestProperties) {
    this.otlpTracesMapper = otlpTracesMapper;
    this.clickHouseService = clickHouseService;
    this.metrics = metrics;
    this.tracesIngestProperties = tracesIngestProperties;
  }

  @KafkaListener(topics = "traces.raw", groupId = "log-processor-traces-group")
  public void consumeTraces(byte[] message) {
    final ExportTraceServiceRequest request;
    try {
      request = ExportTraceServiceRequest.parseFrom(message);
    } catch (InvalidProtocolBufferException e) {
      metrics.recordTraceParseError();
      log.warn("OTLP ExportTraceServiceRequest parse failed: {}", e.getMessage());
      return;
    } catch (RuntimeException e) {
      metrics.recordTraceParseError();
      log.warn("Unexpected error parsing OTLP trace payload: {}", e.getMessage(), e);
      return;
    }

    for (ResourceSpans resourceSpans : request.getResourceSpansList()) {
      otlpTracesMapper.forEachRawSpan(resourceSpans, this::processRawSpan);
    }
  }

  private void processRawSpan(RawSpanEvent event) {
    metrics.recordTraceReceived();
    if (tracesIngestProperties.serverSpansOnly() && !"SERVER".equals(event.span_kind)) {
      return;
    }
    try {
      clickHouseService.insertSpan(event);
      metrics.recordTraceStored();
    } catch (DataAccessException e) {
      metrics.recordTraceStoreError();
      log.error(
          "ClickHouse span insert failed tenant={} service={} traceId={} spanId={} name={}",
          nullToEmpty(event.tenant_id),
          nullToEmpty(event.service_name),
          nullToEmpty(event.trace_id),
          nullToEmpty(event.span_id),
          truncate(nullToEmpty(event.span_name), 120),
          e);
    }
  }

  private static String nullToEmpty(String s) {
    return s == null ? "" : s;
  }

  private static String truncate(String s, int maxLen) {
    if (s.length() <= maxLen) {
      return s;
    }
    return s.substring(0, maxLen) + "...";
  }
}
