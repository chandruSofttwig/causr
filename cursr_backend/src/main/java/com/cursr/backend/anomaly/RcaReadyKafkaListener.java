package com.cursr.backend.anomaly;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
public class RcaReadyKafkaListener {

  private static final Logger log = LoggerFactory.getLogger(RcaReadyKafkaListener.class);

  private final StringRedisTemplate stringRedisTemplate;
  private final ObjectMapper objectMapper;
  private final String redisChannelPrefix;
  private final RcaSlackNotifier rcaSlackNotifier;

  public RcaReadyKafkaListener(
      StringRedisTemplate stringRedisTemplate,
      ObjectMapper objectMapper,
      @Value("${app.anomalies.redis-channel-prefix}") String redisChannelPrefix,
      RcaSlackNotifier rcaSlackNotifier) {
    this.stringRedisTemplate = stringRedisTemplate;
    this.objectMapper = objectMapper;
    this.redisChannelPrefix = redisChannelPrefix;
    this.rcaSlackNotifier = rcaSlackNotifier;
  }

  @KafkaListener(
      topics = "${app.anomalies.rca-ready-topic}",
      groupId = "${spring.kafka.consumer.group-id}-rca")
  public void consumeRcaReady(String json) {
    RcaReadyEvent event;
    try {
      event = objectMapper.readValue(json, RcaReadyEvent.class);
      String tenantId =
          event.tenantId() == null || event.tenantId().isBlank() ? "default" : event.tenantId();
      ObjectNode node = (ObjectNode) objectMapper.readTree(json);
      if (!node.has("type")) {
        node.put("type", "rca-ready");
      }
      stringRedisTemplate.convertAndSend(redisChannelPrefix + ":" + tenantId, node.toString());
    } catch (Exception e) {
      log.warn("RCA-ready Kafka message skipped: {}", e.getMessage());
      return;
    }

    try {
      rcaSlackNotifier.notifyIfEnabled(event);
    } catch (Exception e) {
      log.warn("Slack RCA notify failed: {}", e.getMessage());
    }
  }
}
