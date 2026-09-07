package com.example.logprocessor.scoring;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * EWMA scorer as in architecture spec; persists ewma/stddev in Redis key {@code ewma:{key}}.
 */
@Component
public class EwmaScorer {

  private static final float ALPHA = 0.1f;
  private static final float EPS = 1e-6f;

  private final StringRedisTemplate redis;

  public EwmaScorer(StringRedisTemplate redis) {
    this.redis = redis;
  }

  public float score(String serviceKey, float errorRate) {
    String key = "ewma:" + serviceKey;
    float ewma = errorRate;
    float stddev = 0.01f;
    try {
      String raw = redis.opsForValue().get(key);
      if (raw != null && !raw.isBlank()) {
        String[] p = raw.split(",");
        if (p.length >= 1) {
          ewma = Float.parseFloat(p[0]);
        }
        if (p.length >= 2) {
          stddev = Float.parseFloat(p[1]);
        }
      }
    } catch (DataAccessException | NumberFormatException ignored) {
      // use defaults above
    }
    float newEwma = ALPHA * errorRate + (1 - ALPHA) * ewma;
    float nextStd = ALPHA * Math.abs(errorRate - newEwma) + (1 - ALPHA) * stddev;
    float anomaly = Math.abs(errorRate - newEwma) / (nextStd + EPS);
    try {
      redis.opsForValue().set(key, newEwma + "," + nextStd);
    } catch (DataAccessException ignored) {
      // state not persisted — scoring still returned for this request
    }
    return anomaly;
  }
}
