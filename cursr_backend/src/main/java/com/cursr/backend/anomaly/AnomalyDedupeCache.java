package com.cursr.backend.anomaly;

import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class AnomalyDedupeCache {

  private static final String KEY_PREFIX = "slack:dedupe:";

  private final StringRedisTemplate redis;

  public AnomalyDedupeCache(StringRedisTemplate redis) {
    this.redis = redis;
  }

  /**
   * @return true if this alert should be sent (first in dedupe window), false if suppressed
   */
  public boolean tryAcquire(AnomalyAlertEvent event, Duration ttl) {
    String key = dedupeKey(event);
    Boolean acquired = redis.opsForValue().setIfAbsent(key, "1", ttl);
    return Boolean.TRUE.equals(acquired);
  }

  static String dedupeKey(AnomalyAlertEvent event) {
    String tenant = blankToDefault(event.tenantId(), "default");
    String service = blankToDefault(event.serviceName(), "unknown");
    String env = blankToDefault(event.environment(), "unknown");
    return KEY_PREFIX + tenant + ":" + service + ":" + env;
  }

  private static String blankToDefault(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }
}
