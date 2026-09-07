package com.causr.llmrouter.kafka;

import com.causr.llmrouter.anomaly.AnomalyAlertEvent;
import com.causr.llmrouter.anomaly.RcaGenerationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class AnomalyAlertListener {

  private static final Logger log = LoggerFactory.getLogger(AnomalyAlertListener.class);

  private final ObjectMapper objectMapper;
  private final RcaGenerationService rcaGenerationService;

  public AnomalyAlertListener(ObjectMapper objectMapper, RcaGenerationService rcaGenerationService) {
    this.objectMapper = objectMapper;
    this.rcaGenerationService = rcaGenerationService;
  }

  @KafkaListener(topics = "${app.anomalies.kafka-topic}", groupId = "${spring.kafka.consumer.group-id}")
  public void onAnomaly(String json) {
    try {
      AnomalyAlertEvent event = objectMapper.readValue(json, AnomalyAlertEvent.class);
      rcaGenerationService.generateFor(event);
    } catch (Exception e) {
      log.warn("Failed to process anomaly-alerts message: {}", e.getMessage());
    }
  }
}
