package com.example.logprocessor.web;

import com.example.logprocessor.clickhouse.ClickHouseService;
import com.example.logprocessor.config.AiProperties;
import com.example.logprocessor.kafka.AnomalyAlertPublisher;
import com.example.logprocessor.model.AnomalyKafkaMessage;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnProperty(name = "app.ai.dev-endpoints-enabled", havingValue = "true")
public class DevAnomalyController {

  private final ClickHouseService clickHouseService;
  private final AnomalyAlertPublisher anomalyAlertPublisher;
  private final AiProperties aiProperties;

  public DevAnomalyController(
      ClickHouseService clickHouseService,
      AnomalyAlertPublisher anomalyAlertPublisher,
      AiProperties aiProperties) {
    this.clickHouseService = clickHouseService;
    this.anomalyAlertPublisher = anomalyAlertPublisher;
    this.aiProperties = aiProperties;
  }

  @PostMapping("/api/dev/emit-anomaly")
  public Map<String, Object> emitAnomaly(
      @RequestParam(defaultValue = "default") String tenantId,
      @RequestParam(defaultValue = "log-sender-backend") String serviceName,
      @RequestParam(defaultValue = "prod") String environment,
      @RequestParam(required = false) Float anomalyScore) {

    UUID id = UUID.randomUUID();
    Instant end = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    Instant start = end.minusSeconds(30);
    LocalDateTime windowStart = LocalDateTime.ofInstant(start, ZoneOffset.UTC);
    LocalDateTime windowEnd = LocalDateTime.ofInstant(end, ZoneOffset.UTC);
    float score = anomalyScore != null ? anomalyScore : -1.0f;

    String featureJson =
        """
        {"dev":true,"service_name":"%s","environment":"%s","note":"manual emit for end-to-end RCA demo"}
        """
            .formatted(serviceName, environment)
            .trim();

    clickHouseService.insertAnomaly(
        id, windowStart, windowEnd, tenantId, serviceName, environment, score, true, featureJson);

    anomalyAlertPublisher.publish(
        new AnomalyKafkaMessage(
            id,
            start.toEpochMilli(),
            end.toEpochMilli(),
            tenantId,
            serviceName,
            environment,
            score,
            featureJson));

    Map<String, Object> out = new LinkedHashMap<>();
    out.put("id", id.toString());
    out.put("serviceName", serviceName);
    out.put("environment", environment);
    out.put("anomalyScore", score);
    out.put("publishedTopic", "anomaly-alerts");
    out.put("note", "If ANTHROPIC_API_KEY is set, llm-router-service will write anomalies.rca_text.");
    out.put("devFlags", Map.of(
        "bypassHistoryGate", aiProperties.bypassHistoryGateOrDefault(),
        "forcePublishAnomaly", aiProperties.forcePublishAnomalyOrDefault()
    ));
    return out;
  }
}

