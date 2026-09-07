package com.example.logprocessor.sampling;

import com.example.logprocessor.model.RawLogEvent;
import com.example.logprocessor.model.StorageDecision;
import org.springframework.stereotype.Service;

/**
 * Null-safe: missing {@code log_level} is not treated as ERROR/WARN; missing {@code message} skips
 * keyword matching (random-sample branch still applies).
 */
@Service
public class LogSamplingService {

  private static final double SAMPLE_RATE = 0.1;

  private final RandomSampler randomSampler;

  public LogSamplingService(RandomSampler randomSampler) {
    this.randomSampler = randomSampler;
  }

  public StorageDecision decide(RawLogEvent log) {
    String level = log.log_level == null ? "" : log.log_level.toUpperCase();
    String msg = log.message == null ? "" : log.message.toLowerCase();

    if ("ERROR".equals(level) || "WARN".equals(level)) {
      return StorageDecision.HOT;
    }
    if (log.duration_ms != null && log.duration_ms > 1000) {
      return StorageDecision.HOT;
    }
    if (msg.contains("exception")
        || msg.contains("timeout")
        || msg.contains("error")
        || msg.contains("failed")) {
      return StorageDecision.HOT;
    }
    if (log.anomaly_score != null && log.anomaly_score > 0.7f) {
      return StorageDecision.HOT;
    }
    if (randomSampler.nextUnitDouble() < SAMPLE_RATE) {
      return StorageDecision.COLD;
    }
    return StorageDecision.DROP;
  }
}
