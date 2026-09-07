package com.causr.llmrouter.kafka;

import com.causr.llmrouter.anomaly.RcaReadyEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class RcaReadyPublisher {

  private static final Logger log = LoggerFactory.getLogger(RcaReadyPublisher.class);

  private final KafkaTemplate<String, String> kafkaTemplate;
  private final ObjectMapper objectMapper;
  private final String topic;

  public RcaReadyPublisher(
      KafkaTemplate<String, String> kafkaTemplate,
      ObjectMapper objectMapper,
      @Value("${app.anomalies.rca-ready-topic}") String topic) {
    this.kafkaTemplate = kafkaTemplate;
    this.objectMapper = objectMapper;
    this.topic = topic;
  }

  public void publish(RcaReadyEvent event) {
    try {
      String key = event.serviceName() == null ? "unknown" : event.serviceName();
      String json = objectMapper.writeValueAsString(event);
      kafkaTemplate.send(topic, key, json);
      log.info("Published rca-ready for anomaly id={}", event.id());
    } catch (Exception e) {
      log.warn("Failed to publish rca-ready for id={}: {}", event.id(), e.getMessage());
    }
  }
}
