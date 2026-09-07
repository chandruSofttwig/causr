package com.cursr.backend.anomaly;

import com.cursr.backend.slack.SlackProperties;
import com.cursr.backend.slack.SlackRcaMessageBuilder;
import com.cursr.backend.slack.SlackWebhookClient;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RcaSlackNotifier {

  private static final Logger log = LoggerFactory.getLogger(RcaSlackNotifier.class);
  private static final String KEY_PREFIX = "slack:rca:";

  private final SlackProperties slackProperties;
  private final SlackRcaMessageBuilder messageBuilder;
  private final SlackWebhookClient webhookClient;
  private final StringRedisTemplate redis;

  public RcaSlackNotifier(
      SlackProperties slackProperties,
      SlackRcaMessageBuilder messageBuilder,
      SlackWebhookClient webhookClient,
      StringRedisTemplate redis) {
    this.slackProperties = slackProperties;
    this.messageBuilder = messageBuilder;
    this.webhookClient = webhookClient;
    this.redis = redis;
  }

  public void notifyIfEnabled(RcaReadyEvent event) {
    if (!slackProperties.enabledOrDefault() || !slackProperties.hasWebhook()) {
      return;
    }
    if (event == null || event.rcaText() == null || event.rcaText().isBlank()) {
      return;
    }
    String tenant = blank(event.tenantId(), "default");
    String service = blank(event.serviceName(), "unknown");
    String env = blank(event.environment(), "unknown");
    String key = KEY_PREFIX + tenant + ":" + service + ":" + env;
    Duration ttl = Duration.ofMinutes(slackProperties.dedupeWindowMinutesOrDefault());
    Boolean acquired = redis.opsForValue().setIfAbsent(key, "1", ttl);
    if (!Boolean.TRUE.equals(acquired)) {
      log.debug("Slack RCA suppressed (dedupe): {} {} {}", tenant, service, env);
      return;
    }
    try {
      var payload = messageBuilder.buildBlocks(event, slackProperties.dashboardBaseUrlOrDefault());
      webhookClient.post(slackProperties.webhookUrl(), payload);
      log.info("Slack RCA alert sent for {} ({}) id={}", service, env, event.id());
    } catch (Exception e) {
      log.warn("Slack RCA alert failed for {} ({}): {}", service, env, e.getMessage());
    }
  }

  private static String blank(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }
}
