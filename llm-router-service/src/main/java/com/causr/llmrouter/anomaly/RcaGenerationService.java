package com.causr.llmrouter.anomaly;

import com.causr.llmrouter.clickhouse.AnomalyRcaRepository;
import com.causr.llmrouter.kafka.RcaReadyPublisher;
import com.causr.llmrouter.llm.AnthropicRcaClient;
import com.causr.llmrouter.llm.RcaPromptBuilder;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RcaGenerationService {

  private static final Logger log = LoggerFactory.getLogger(RcaGenerationService.class);

  private final AnomalyRcaRepository repository;
  private final RcaPromptBuilder promptBuilder;
  private final AnthropicRcaClient anthropicRcaClient;
  private final RcaReadyPublisher rcaReadyPublisher;
  private final int maxContextLogs;
  private final boolean enabled;

  public RcaGenerationService(
      AnomalyRcaRepository repository,
      RcaPromptBuilder promptBuilder,
      AnthropicRcaClient anthropicRcaClient,
      RcaReadyPublisher rcaReadyPublisher,
      @Value("${app.rca.max-context-logs:25}") int maxContextLogs,
      @Value("${app.rca.enabled:true}") boolean enabled) {
    this.repository = repository;
    this.promptBuilder = promptBuilder;
    this.anthropicRcaClient = anthropicRcaClient;
    this.rcaReadyPublisher = rcaReadyPublisher;
    this.maxContextLogs = maxContextLogs;
    this.enabled = enabled;
  }

  public void generateFor(AnomalyAlertEvent event) {
    if (!enabled) {
      log.debug("RCA generation disabled");
      return;
    }
    if (event == null || event.id() == null) {
      log.warn("Skipping RCA: anomaly id missing");
      return;
    }
    if (!anthropicRcaClient.isConfigured()) {
      log.warn(
          "Skipping RCA for anomaly id={}: ANTHROPIC_API_KEY not set (set key to enable)",
          event.id());
      return;
    }

    String service = blankTo(event.serviceName(), "unknown");
    String env = blankTo(event.environment(), "");
    List<Map<String, Object>> logs =
        repository.findRecentErrorLogs(
            service, env, event.windowStartEpochMs(), event.windowEndEpochMs(), maxContextLogs);

    String prompt =
        promptBuilder.buildUserPrompt(
            service, env, event.anomalyScore(), event.featureJson(), logs);
    String rcaText = anthropicRcaClient.generateRca(prompt);
    if (rcaText == null || rcaText.isBlank()) {
      log.warn("Empty RCA from Anthropic for anomaly id={}", event.id());
      return;
    }

    repository.updateRca(event.id(), rcaText);
    String generatedAt = Instant.now().toString();
    rcaReadyPublisher.publish(RcaReadyEvent.from(event, rcaText, generatedAt));
    log.info("RCA written for anomaly id={} service={}", event.id(), service);
  }

  private static String blankTo(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }
}
