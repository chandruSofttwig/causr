package com.cursr.backend.anomaly;

import com.cursr.backend.slack.SlackAnomalyMessageBuilder;
import com.cursr.backend.slack.SlackProperties;
import com.cursr.backend.slack.SlackWebhookClient;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AnomalySlackNotifier {

  private static final Logger log = LoggerFactory.getLogger(AnomalySlackNotifier.class);

  private final SlackProperties slackProperties;
  private final SlackAnomalyMessageBuilder messageBuilder;
  private final SlackWebhookClient webhookClient;
  private final AnomalyDedupeCache dedupeCache;

  public AnomalySlackNotifier(
      SlackProperties slackProperties,
      SlackAnomalyMessageBuilder messageBuilder,
      SlackWebhookClient webhookClient,
      AnomalyDedupeCache dedupeCache) {
    this.slackProperties = slackProperties;
    this.messageBuilder = messageBuilder;
    this.webhookClient = webhookClient;
    this.dedupeCache = dedupeCache;
  }

  public void notifyIfEnabled(AnomalyAlertEvent event) {
    if (!slackProperties.enabledOrDefault() || !slackProperties.hasWebhook()) {
      return;
    }
    Duration ttl = Duration.ofMinutes(slackProperties.dedupeWindowMinutesOrDefault());
    if (!dedupeCache.tryAcquire(event, ttl)) {
      log.debug(
          "Slack alert suppressed (dedupe): {} {} {}",
          event.tenantId(),
          event.serviceName(),
          event.environment());
      return;
    }
    try {
      var payload =
          messageBuilder.buildBlocks(event, slackProperties.dashboardBaseUrlOrDefault());
      webhookClient.post(slackProperties.webhookUrl(), payload);
      log.info(
          "Slack anomaly alert sent for {} ({}) id={}",
          event.serviceName(),
          event.environment(),
          event.id());
    } catch (Exception e) {
      log.warn(
          "Slack anomaly alert failed for {} ({}): {}",
          event.serviceName(),
          event.environment(),
          e.getMessage());
    }
  }
}
