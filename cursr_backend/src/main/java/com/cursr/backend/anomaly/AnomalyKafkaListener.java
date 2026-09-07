package com.cursr.backend.anomaly;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
@ConditionalOnProperty(name = "app.anomalies.kafka-consumer-enabled", havingValue = "true")
public class AnomalyKafkaListener {

  private static final Logger log = LoggerFactory.getLogger(AnomalyKafkaListener.class);

  private final StringRedisTemplate stringRedisTemplate;
  private final ObjectMapper objectMapper;
  private final String redisChannelPrefix;
  private final AnomalySlackNotifier anomalySlackNotifier;

  public AnomalyKafkaListener(
      StringRedisTemplate stringRedisTemplate,
      ObjectMapper objectMapper,
      @Value("${app.anomalies.redis-channel-prefix}") String redisChannelPrefix,
      AnomalySlackNotifier anomalySlackNotifier) {
    this.stringRedisTemplate = stringRedisTemplate;
    this.objectMapper = objectMapper;
    this.redisChannelPrefix = redisChannelPrefix;
    this.anomalySlackNotifier = anomalySlackNotifier;
  }

  @KafkaListener(topics = "${app.anomalies.kafka-topic}", groupId = "${spring.kafka.consumer.group-id}")
  public void consumeAnomaly(String json) {
    AnomalyAlertEvent event = null;
    try {
      event = objectMapper.readValue(json, AnomalyAlertEvent.class);
      String tenantId = resolveTenantId(event);
      String channel = redisChannelPrefix + ":" + tenantId;
      stringRedisTemplate.convertAndSend(channel, json);
    } catch (Exception e) {
      log.warn("Anomaly Kafka message skipped: {}", e.getMessage());
      return;
    }

    try {
      anomalySlackNotifier.notifyIfEnabled(event);
    } catch (Exception e) {
      log.warn("Slack notify after anomaly Kafka message failed: {}", e.getMessage());
    }
  }

  private String resolveTenantId(AnomalyAlertEvent event) {
    if (event.tenantId() != null && !event.tenantId().isBlank()) {
      return event.tenantId();
    }
    return "default";
  }
}
