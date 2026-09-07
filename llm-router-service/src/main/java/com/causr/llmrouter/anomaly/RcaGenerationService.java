package com.causr.llmrouter.anomaly;

import com.causr.llmrouter.clickhouse.AnomalyRcaRepository;
import com.causr.llmrouter.kafka.RcaReadyPublisher;
import com.causr.llmrouter.llm.GroqRcaClient;
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
  private final GroqRcaClient groqRcaClient;
  private final RcaReadyPublisher rcaReadyPublisher;
  private final int maxContextLogs;
  private final boolean enabled;

  public RcaGenerationService(
      AnomalyRcaRepository repository,
      RcaPromptBuilder promptBuilder,
      GroqRcaClient groqRcaClient,
      RcaReadyPublisher rcaReadyPublisher,
      @Value("${app.rca.max-context-logs:25}") int maxContextLogs,
      @Value("${app.rca.enabled:true}") boolean enabled) {
    this.repository = repository;
    this.promptBuilder = promptBuilder;
    this.groqRcaClient = groqRcaClient;
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
    if (!groqRcaClient.isConfigured()) {
      log.warn(
          "Skipping RCA for anomaly id={}: GROQ_API_KEY not set (set key to enable Groq RCA)",
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
    String rcaText = groqRcaClient.generateRca(prompt);
    if (rcaText == null || rcaText.isBlank()) {
      log.warn("Empty RCA from Groq ({}) for anomaly id={}", groqRcaClient.getModel(), event.id());
      return;
    }

    repository.updateRca(event.id(), rcaText);
    String generatedAt = Instant.now().toString();
    rcaReadyPublisher.publish(RcaReadyEvent.from(event, rcaText, generatedAt));
    log.info(
        "RCA written for anomaly id={} service={} model={}",
        event.id(),
        service,
        groqRcaClient.getModel());
  }

  private static String blankTo(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }
}
