package com.example.logprocessor.sampling;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.logprocessor.model.RawLogEvent;
import com.example.logprocessor.model.StorageDecision;
import org.junit.jupiter.api.Test;

class LogSamplingServiceTest {

  @Test
  void errorLevelIsHotRegardlessOfRandom() {
    LogSamplingService svc = new LogSamplingService(() -> 0.99);
    RawLogEvent e = new RawLogEvent();
    e.log_level = "error";
    assertEquals(StorageDecision.HOT, svc.decide(e));
  }

  @Test
  void warnLevelIsHot() {
    LogSamplingService svc = new LogSamplingService(() -> 0.99);
    RawLogEvent e = new RawLogEvent();
    e.log_level = "WARN";
    assertEquals(StorageDecision.HOT, svc.decide(e));
  }

  @Test
  void durationAbove1000IsHot() {
    LogSamplingService svc = new LogSamplingService(() -> 0.99);
    RawLogEvent e = new RawLogEvent();
    e.log_level = "INFO";
    e.duration_ms = 1001;
    assertEquals(StorageDecision.HOT, svc.decide(e));
  }

  @Test
  void durationExactly1000IsNotHotByDuration() {
    LogSamplingService svc = new LogSamplingService(() -> 0.99);
    RawLogEvent e = new RawLogEvent();
    e.log_level = "INFO";
    e.message = "ok";
    e.duration_ms = 1000;
    assertEquals(StorageDecision.DROP, svc.decide(e));
  }

  @Test
  void messageKeywordsAreHot() {
    LogSamplingService svc = new LogSamplingService(() -> 0.99);
    RawLogEvent e = new RawLogEvent();
    e.log_level = "INFO";
    e.message = "ReadTimeout occurred";
    assertEquals(StorageDecision.HOT, svc.decide(e));
  }

  @Test
  void anomalyScoreAboveThresholdIsHot() {
    LogSamplingService svc = new LogSamplingService(() -> 0.99);
    RawLogEvent e = new RawLogEvent();
    e.log_level = "INFO";
    e.message = "x";
    e.anomaly_score = 0.71f;
    assertEquals(StorageDecision.HOT, svc.decide(e));
  }

  @Test
  void anomalyScoreAtThresholdIsNotHot() {
    LogSamplingService svc = new LogSamplingService(() -> 0.99);
    RawLogEvent e = new RawLogEvent();
    e.log_level = "INFO";
    e.message = "x";
    e.anomaly_score = 0.7f;
    assertEquals(StorageDecision.DROP, svc.decide(e));
  }

  @Test
  void nullLevelAndMessageUsesRandomSampleCold() {
    LogSamplingService svc = new LogSamplingService(() -> 0.05);
    RawLogEvent e = new RawLogEvent();
    assertEquals(StorageDecision.COLD, svc.decide(e));
  }

  @Test
  void nullLevelAndMessageUsesRandomSampleDrop() {
    LogSamplingService svc = new LogSamplingService(() -> 0.5);
    RawLogEvent e = new RawLogEvent();
    assertEquals(StorageDecision.DROP, svc.decide(e));
  }
}
