package com.example.logprocessor.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class LogProcessingMetrics {

  private final Counter received;
  private final Counter parseErrors;
  private final Counter storedHot;
  private final Counter storedCold;
  private final Counter dropped;
  private final Counter storeErrorsHot;
  private final Counter storeErrorsCold;
  private final Counter tracesReceived;
  private final Counter tracesParseErrors;
  private final Counter tracesStored;
  private final Counter tracesStoreErrors;

  public LogProcessingMetrics(MeterRegistry registry) {
    this.received = registry.counter("logs.received");
    this.parseErrors = registry.counter("logs.parse.errors");
    this.storedHot = Counter.builder("logs.stored").tag("tier", "hot").register(registry);
    this.storedCold = Counter.builder("logs.stored").tag("tier", "cold").register(registry);
    this.dropped = registry.counter("logs.dropped");
    this.storeErrorsHot =
        Counter.builder("logs.store.errors").tag("tier", "hot").register(registry);
    this.storeErrorsCold =
        Counter.builder("logs.store.errors").tag("tier", "cold").register(registry);
    this.tracesReceived = registry.counter("traces.received");
    this.tracesParseErrors = registry.counter("traces.parse.errors");
    this.tracesStored = registry.counter("traces.stored");
    this.tracesStoreErrors = registry.counter("traces.store.errors");
    Gauge.builder("logs.sampling.rate", this, LogProcessingMetrics::samplingRate).register(registry);
  }

  private double samplingRate() {
    double stored = storedHot.count() + storedCold.count();
    double drop = dropped.count();
    double denom = stored + drop;
    return denom == 0.0 ? 0.0 : stored / denom;
  }

  public void recordReceived() {
    received.increment();
  }

  public void recordParseError() {
    parseErrors.increment();
  }

  public void recordStoredHot() {
    storedHot.increment();
  }

  public void recordStoredCold() {
    storedCold.increment();
  }

  public void recordDropped() {
    dropped.increment();
  }

  /** Persisting to ClickHouse failed after sampling chose hot tier. */
  public void recordStoreErrorHot() {
    storeErrorsHot.increment();
  }

  /** Persisting to ClickHouse failed after sampling chose cold tier. */
  public void recordStoreErrorCold() {
    storeErrorsCold.increment();
  }

  public void recordTraceReceived() {
    tracesReceived.increment();
  }

  public void recordTraceParseError() {
    tracesParseErrors.increment();
  }

  public void recordTraceStored() {
    tracesStored.increment();
  }

  public void recordTraceStoreError() {
    tracesStoreErrors.increment();
  }
}
