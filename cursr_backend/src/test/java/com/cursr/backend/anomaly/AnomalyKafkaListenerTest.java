package com.cursr.backend.anomaly;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class AnomalyKafkaListenerTest {

  @Mock private StringRedisTemplate stringRedisTemplate;
  @Mock private AnomalySlackNotifier anomalySlackNotifier;

  private final ObjectMapper objectMapper = new ObjectMapper();
  private AnomalyKafkaListener listener;

  @BeforeEach
  void setUp() {
    listener =
        new AnomalyKafkaListener(
            stringRedisTemplate, objectMapper, "anomaly", anomalySlackNotifier);
  }

  @Test
  void consumeAnomaly_fansOutToRedisAndNotifiesSlack() throws Exception {
    UUID id = UUID.randomUUID();
    AnomalyAlertEvent event =
        new AnomalyAlertEvent(id, 1000L, 2000L, "", "payment-service", "staging", -0.4f, "{}");
    String json = objectMapper.writeValueAsString(event);

    listener.consumeAnomaly(json);

    verify(stringRedisTemplate).convertAndSend("anomaly:default", json);
    verify(anomalySlackNotifier).notifyIfEnabled(any(AnomalyAlertEvent.class));
  }

  @Test
  void consumeAnomaly_redisStillWorksWhenSlackFails() throws Exception {
    UUID id = UUID.randomUUID();
    AnomalyAlertEvent event =
        new AnomalyAlertEvent(id, 1000L, 2000L, "tenant-a", "auth-service", "prod", -0.3f, "{}");
    String json = objectMapper.writeValueAsString(event);
    doThrow(new RuntimeException("slack down"))
        .when(anomalySlackNotifier)
        .notifyIfEnabled(any(AnomalyAlertEvent.class));

    listener.consumeAnomaly(json);

    verify(stringRedisTemplate).convertAndSend("anomaly:tenant-a", json);
    verify(anomalySlackNotifier).notifyIfEnabled(any(AnomalyAlertEvent.class));
  }
}
