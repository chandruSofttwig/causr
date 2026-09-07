package com.example.logprocessor.kafka;

import com.example.logprocessor.model.AnomalyKafkaMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class AnomalyAlertPublisher {

  private static final Logger log = LoggerFactory.getLogger(AnomalyAlertPublisher.class);

  private final KafkaTemplate<String, String> kafka;
  private final ObjectMapper objectMapper;

  public AnomalyAlertPublisher(
      KafkaTemplate<String, String> kafka, ObjectMapper objectMapper) {
    this.kafka = kafka;
    this.objectMapper = objectMapper;
  }

  public void publish(AnomalyKafkaMessage message) {
    try {
      String json = objectMapper.writeValueAsString(message);
      kafka.send("anomaly-alerts", message.serviceName(), json);
    } catch (JsonProcessingException e) {
      log.warn("Failed to serialize anomaly alert: {}", e.getMessage());
    }
  }
}
