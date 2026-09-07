package com.example.logprocessor.streams;

import com.example.logprocessor.ai.FeatureVector;
import com.example.logprocessor.ai.GrpcAnomalyScorerClient;
import com.example.logprocessor.ai.ScoreResult;
import com.example.logprocessor.clickhouse.ClickHouseService;
import com.example.logprocessor.config.AiProperties;
import com.example.logprocessor.kafka.AnomalyAlertPublisher;
import com.example.logprocessor.model.AnomalyKafkaMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.apache.kafka.streams.kstream.Windowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class ServiceMetricsWindowHandler {

  private static final Logger log = LoggerFactory.getLogger(ServiceMetricsWindowHandler.class);

  private final ClickHouseService clickHouseService;
  private final GrpcAnomalyScorerClient grpcAnomalyScorerClient;
  private final ServiceMetricsHistoryChecker historyChecker;
  private final StringRedisTemplate redis;
  private final AnomalyAlertPublisher anomalyAlertPublisher;
  private final ObjectMapper objectMapper;
  private final AiProperties aiProperties;

  public ServiceMetricsWindowHandler(
      ClickHouseService clickHouseService,
      GrpcAnomalyScorerClient grpcAnomalyScorerClient,
      ServiceMetricsHistoryChecker historyChecker,
      StringRedisTemplate redis,
      AnomalyAlertPublisher anomalyAlertPublisher,
      ObjectMapper objectMapper,
      AiProperties aiProperties) {
    this.clickHouseService = clickHouseService;
    this.grpcAnomalyScorerClient = grpcAnomalyScorerClient;
    this.historyChecker = historyChecker;
    this.redis = redis;
    this.anomalyAlertPublisher = anomalyAlertPublisher;
    this.objectMapper = objectMapper;
    this.aiProperties = aiProperties;
  }

  public void handle(Windowed<String> windowedKey, FeatureAccumulator acc) {
    try {
      String groupKey = windowedKey.key();
      int hash = groupKey.lastIndexOf('#');
      String serviceName = hash < 0 ? groupKey : groupKey.substring(0, hash);
      String environment = hash < 0 ? "unknown" : groupKey.substring(hash + 1);

      LocalDateTime windowStart =
          LocalDateTime.ofInstant(
              Instant.ofEpochMilli(windowedKey.window().start()), ZoneOffset.UTC);
      LocalDateTime windowEnd =
          LocalDateTime.ofInstant(
              Instant.ofEpochMilli(windowedKey.window().end()), ZoneOffset.UTC);

      long logVolume = acc.totalCount;
      long errorVolume = acc.errorCount;
      float errorRate = logVolume > 0 ? (float) errorVolume / logVolume : 0f;
      float p99 = 0f;
      try {
        if (logVolume > 0) {
          p99 = (float) acc.digest().quantile(0.99);
        }
      } catch (RuntimeException ignored) {
        p99 = 0f;
      }
      int uniqueErrorTypes = acc.errorTemplates.size();
      int newTemplates = countNewTemplates(groupKey, acc);
      byte silenceFlag = logVolume == 0 ? (byte) 1 : (byte) 0;
      byte deploymentFlag = 0;
      int hour = windowEnd.getHour();
      float sin = (float) Math.sin(2 * Math.PI * hour / 24.0);
      float cos = (float)(Math.cos(2 * Math.PI * hour / 24.0));

      float aiScore = 0f;
      boolean isAnomaly = false;
      boolean hasBaseline = historyChecker.hasAtLeastSevenDays(serviceName, environment);
      boolean shouldScore = hasBaseline || aiProperties.bypassHistoryGateOrDefault();
      if (shouldScore) {
        FeatureVector fv =
            FeatureVector.newBuilder()
                .setTenantId(nullToEmpty(acc.tenantId))
                .setServiceName(serviceName)
                .setEnvironment(environment)
                .setWindowStartUnixMs(windowedKey.window().start())
                .setWindowEndUnixMs(windowedKey.window().end())
                .setLogVolume(logVolume)
                .setErrorVolume(errorVolume)
                .setErrorRate(errorRate)
                .setP99LatencyMs(p99)
                .setUniqueErrorTypes(uniqueErrorTypes)
                .setNewErrorTypes(newTemplates)
                .setSilenceFlag(silenceFlag)
                .setDeploymentFlag(deploymentFlag)
                .setTimeOfDaySin(sin)
                .setTimeOfDayCos(cos)
                .build();
        Optional<ScoreResult> scored = grpcAnomalyScorerClient.scoreOne(fv);
        if (scored.isPresent()) {
          aiScore = scored.get().getAnomalyScore();
          isAnomaly = scored.get().getIsAnomaly();
          if (aiProperties.anomalyThresholdOverride() != null) {
            isAnomaly = aiScore < aiProperties.anomalyThresholdOverride();
          }
        }
      }

      ServiceFeatureRow row =
          new ServiceFeatureRow(
              windowStart,
              windowEnd,
              nullToEmpty(acc.tenantId),
              serviceName,
              environment,
              logVolume,
              errorVolume,
              errorRate,
              p99,
              uniqueErrorTypes,
              newTemplates,
              silenceFlag,
              deploymentFlag,
              sin,
              cos,
              aiScore);
      clickHouseService.insertServiceMetrics(row);

      if (isAnomaly || aiProperties.forcePublishAnomalyOrDefault()) {
        UUID id = UUID.randomUUID();
        String featureJson = toJson(row);
        clickHouseService.insertAnomaly(
            id, windowStart, windowEnd, acc.tenantId, serviceName, environment, aiScore, true, featureJson);
        anomalyAlertPublisher.publish(
            new AnomalyKafkaMessage(
                id,
                windowedKey.window().start(),
                windowedKey.window().end(),
                nullToEmpty(acc.tenantId),
                serviceName,
                environment,
                aiScore,
                featureJson));
      }
    } catch (Exception e) {
      log.warn("service_metrics window handler failed: {}", e.getMessage(), e);
    }
  }

  private int countNewTemplates(String groupKey, FeatureAccumulator acc) {
    String tplKey = "templates:7d:" + groupKey;
    int newTemplates = 0;
    for (String h : acc.errorTemplates) {
      Boolean m = redis.opsForSet().isMember(tplKey, h);
      if (!Boolean.TRUE.equals(m)) {
        newTemplates++;
      }
    }
    if (!acc.errorTemplates.isEmpty()) {
      redis.opsForSet().add(tplKey, acc.errorTemplates.toArray(new String[0]));
    }
    redis.expire(tplKey, Duration.ofDays(7));
    return newTemplates;
  }

  private String toJson(ServiceFeatureRow row) {
    try {
      Map<String, Object> m = new HashMap<>();
      m.put("window_start", row.windowStartUtc().toString());
      m.put("window_end", row.windowEndUtc().toString());
      m.put("tenant_id", row.tenantId());
      m.put("service_name", row.serviceName());
      m.put("environment", row.environment());
      m.put("log_volume", row.logVolume());
      m.put("error_volume", row.errorVolume());
      m.put("error_rate", row.errorRate());
      m.put("p99_latency_ms", row.p99LatencyMs());
      m.put("unique_error_types", row.uniqueErrorTypes());
      m.put("new_error_types", row.newErrorTypes());
      m.put("silence_flag", row.silenceFlag());
      m.put("deployment_flag", row.deploymentFlag());
      m.put("time_of_day_sin", row.timeOfDaySin());
      m.put("time_of_day_cos", row.timeOfDayCos());
      m.put("ai_anomaly_score", row.aiAnomalyScore());
      return objectMapper.writeValueAsString(m);
    } catch (JsonProcessingException e) {
      return "{}";
    }
  }

  private static String nullToEmpty(String s) {
    return s == null ? "" : s;
  }
}
