package com.cursr.backend.anomaly;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cursr.backend.slack.SlackAnomalyMessageBuilder;
import com.cursr.backend.slack.SlackProperties;
import com.cursr.backend.slack.SlackWebhookClient;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnomalySlackNotifierTest {

  @Mock private SlackAnomalyMessageBuilder messageBuilder;
  @Mock private SlackWebhookClient webhookClient;
  @Mock private AnomalyDedupeCache dedupeCache;

  private AnomalySlackNotifier notifier;
  private AnomalyAlertEvent event;

  @BeforeEach
  void setUp() {
    event =
        new AnomalyAlertEvent(
            UUID.randomUUID(),
            1L,
            2L,
            "t1",
            "api-gateway",
            "prod",
            -0.5f,
            "{}");
  }

  @Test
  void notifyIfEnabled_skipsWhenSlackDisabled() {
    notifier =
        new AnomalySlackNotifier(
            new SlackProperties(false, "https://hooks.slack.com/test", "http://localhost:5173", 5),
            messageBuilder,
            webhookClient,
            dedupeCache);

    notifier.notifyIfEnabled(event);

    verify(dedupeCache, never()).tryAcquire(any(), any());
    verify(webhookClient, never()).post(any(), any());
  }

  @Test
  void notifyIfEnabled_skipsWhenDedupeSuppresses() {
    notifier =
        new AnomalySlackNotifier(
            new SlackProperties(true, "https://hooks.slack.com/test", "http://localhost:5173", 5),
            messageBuilder,
            webhookClient,
            dedupeCache);
    when(dedupeCache.tryAcquire(eq(event), any(Duration.class))).thenReturn(false);

    notifier.notifyIfEnabled(event);

    verify(webhookClient, never()).post(any(), any());
  }

  @Test
  void notifyIfEnabled_postsWhenEnabledAndNotDeduped() {
    SlackProperties props =
        new SlackProperties(true, "https://hooks.slack.com/test", "http://localhost:5173", 5);
    notifier = new AnomalySlackNotifier(props, messageBuilder, webhookClient, dedupeCache);
    when(dedupeCache.tryAcquire(eq(event), any(Duration.class))).thenReturn(true);
    Map<String, Object> payload = Map.of("blocks", java.util.List.of());
    when(messageBuilder.buildBlocks(event, "http://localhost:5173")).thenReturn(payload);

    notifier.notifyIfEnabled(event);

    verify(webhookClient).post("https://hooks.slack.com/test", payload);
  }

  @Test
  void dedupeKey_groupsByTenantServiceEnvironment() {
    AnomalyAlertEvent e =
        new AnomalyAlertEvent(UUID.randomUUID(), 0, 0, "t", "svc", "env", 0f, null);
    assertThat(AnomalyDedupeCache.dedupeKey(e)).isEqualTo("slack:dedupe:t:svc:env");
  }
}
