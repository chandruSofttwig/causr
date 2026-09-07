package com.example.logprocessor.kafka;

import com.example.logprocessor.ai.LogClusteringClient;
import com.example.logprocessor.clickhouse.ClickHouseService;
import com.example.logprocessor.config.AiProperties;
import com.example.logprocessor.metrics.LogProcessingMetrics;
import com.example.logprocessor.model.RawLogEvent;
import com.example.logprocessor.model.StorageDecision;
import com.example.logprocessor.sampling.LogSamplingService;
import com.example.logprocessor.scoring.EwmaScorer;
import com.google.protobuf.InvalidProtocolBufferException;
import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.logs.v1.ResourceLogs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class LogConsumer {

  private static final Logger log = LoggerFactory.getLogger(LogConsumer.class);

  private final OtlpLogsMapper otlpLogsMapper;
  private final LogSamplingService samplingService;
  private final ClickHouseService clickHouseService;
  private final LogProcessingMetrics metrics;
  private final EwmaScorer ewmaScorer;
  private final LogClusteringClient clusteringClient;
  private final AiProperties aiProperties;

  public LogConsumer(
      OtlpLogsMapper otlpLogsMapper,
      LogSamplingService samplingService,
      ClickHouseService clickHouseService,
      LogProcessingMetrics metrics,
      EwmaScorer ewmaScorer,
      LogClusteringClient clusteringClient,
      AiProperties aiProperties) {
    this.otlpLogsMapper = otlpLogsMapper;
    this.samplingService = samplingService;
    this.clickHouseService = clickHouseService;
    this.metrics = metrics;
    this.ewmaScorer = ewmaScorer;
    this.clusteringClient = clusteringClient;
    this.aiProperties = aiProperties;
  }

  @KafkaListener(topics = "logs.raw", groupId = "log-processor-group")
  public void consumeLogs(byte[] message) {
    final ExportLogsServiceRequest request;
    try {
      request = ExportLogsServiceRequest.parseFrom(message);
    } catch (InvalidProtocolBufferException e) {
      metrics.recordParseError();
      log.warn(
          "OTLP ExportLogsServiceRequest parse failed (malformed or wrong wire format): {}",
          e.getMessage());
      return;
    } catch (RuntimeException e) {
      metrics.recordParseError();
      log.warn("Unexpected error parsing OTLP payload: {}", e.getMessage(), e);
      return;
    }

    for (ResourceLogs resourceLogs : request.getResourceLogsList()) {
      otlpLogsMapper.forEachRawEvent(resourceLogs, this::processRawEvent);
    }
  }

  /**
   * One Kafka message can carry many log records; store failures are isolated per record so one bad
   * row does not fail the entire batch. Failed rows are counted in {@code logs.store.errors} and
   * logged with tenant/service/trace for investigation (data loss for that row unless replayed).
   */
  private void processRawEvent(RawLogEvent event) {
    String ewmaKey =
        nullToEmpty(event.tenant_id)
            + ":"
            + nullToEmpty(event.service_name)
            + "#"
            + nullToEmpty(event.environment);
    float errSignal =
        "ERROR".equalsIgnoreCase(nullToEmpty(event.log_level)) ? 1f : 0f;
    event.anomaly_score = ewmaScorer.score(ewmaKey, errSignal);

    if (aiProperties.clusteringEnabled()) {
      LogClusteringClient.ClusterResponse cr = clusteringClient.cluster(event.message);
      if (cr != null && cr.clusterId != null) {
        event.cluster_id = String.valueOf(cr.clusterId);
        if (event.attributes != null && cr.template != null) {
          event.attributes.put("drain3.template", cr.template);
        }
      }
    }

    metrics.recordReceived();

    StorageDecision decision = samplingService.decide(event);
    if (decision == StorageDecision.DROP) {
      metrics.recordDropped();
      return;
    }

    try {
      if (decision == StorageDecision.HOT) {
        clickHouseService.insertHot(event);
        metrics.recordStoredHot();
      } else {
        clickHouseService.insertCold(event);
        metrics.recordStoredCold();
      }
    } catch (DataAccessException e) {
      if (decision == StorageDecision.HOT) {
        metrics.recordStoreErrorHot();
      } else {
        metrics.recordStoreErrorCold();
      }
      log.error(
          "ClickHouse insert failed tenant={} service={} traceId={} spanId={} tier={} message={}",
          nullToEmpty(event.tenant_id),
          nullToEmpty(event.service_name),
          nullToEmpty(event.trace_id),
          nullToEmpty(event.span_id),
          decision,
          truncate(nullToEmpty(event.message), 200),
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
