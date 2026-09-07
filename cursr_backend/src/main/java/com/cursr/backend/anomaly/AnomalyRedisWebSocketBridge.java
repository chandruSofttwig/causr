package com.cursr.backend.anomaly;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class AnomalyRedisWebSocketBridge implements MessageListener {

  private static final Logger log = LoggerFactory.getLogger(AnomalyRedisWebSocketBridge.class);

  private final SimpMessagingTemplate messagingTemplate;
  private final ObjectMapper objectMapper;
  private final String channelPrefixWithColon;

  public AnomalyRedisWebSocketBridge(
      SimpMessagingTemplate messagingTemplate,
      ObjectMapper objectMapper,
      @Value("${app.anomalies.redis-channel-prefix}") String redisChannelPrefix) {
    this.messagingTemplate = messagingTemplate;
    this.objectMapper = objectMapper;
    this.channelPrefixWithColon = redisChannelPrefix + ":";
  }

  @Override
  public void onMessage(Message message, byte[] pattern) {
    String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
    if (!channel.startsWith(channelPrefixWithColon)) {
      return;
    }
    String tenantId = channel.substring(channelPrefixWithColon.length());
    if (tenantId.isBlank()) {
      return;
    }
    String body = new String(message.getBody(), StandardCharsets.UTF_8);
    try {
      Object payload = objectMapper.readValue(body, Object.class);
      messagingTemplate.convertAndSend("/topic/anomalies/" + tenantId, payload);
    } catch (Exception e) {
      log.warn("Redis anomaly fan-out failed: {}", e.getMessage());
    }
  }
}
