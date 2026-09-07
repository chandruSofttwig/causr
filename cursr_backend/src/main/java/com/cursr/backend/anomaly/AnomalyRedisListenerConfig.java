package com.cursr.backend.anomaly;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
@Profile("!test")
public class AnomalyRedisListenerConfig {

  @Bean
  RedisMessageListenerContainer anomalyRedisMessageListenerContainer(
      RedisConnectionFactory connectionFactory,
      AnomalyRedisWebSocketBridge listener,
      @Value("${app.anomalies.redis-channel-prefix}") String redisChannelPrefix) {
    RedisMessageListenerContainer container = new RedisMessageListenerContainer();
    container.setConnectionFactory(connectionFactory);
    container.addMessageListener(listener, new PatternTopic(redisChannelPrefix + ":*"));
    return container;
  }
}
