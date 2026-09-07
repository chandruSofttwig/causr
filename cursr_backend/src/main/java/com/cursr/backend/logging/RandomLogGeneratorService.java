package com.cursr.backend.logging;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.Severity;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(
  prefix = "app.log-generator",
  name = "enabled",
  havingValue = "true",
  matchIfMissing = true
)
public class RandomLogGeneratorService {

  private static final org.slf4j.Logger APPLICATION_LOGGER = LoggerFactory.getLogger(RandomLogGeneratorService.class);

  private static final List<String> EVENTS = List.of(
    "User login attempt",
    "Order checkout simulation",
    "Inventory sync batch",
    "Payment authorization check",
    "Email notification dispatch",
    "Session refresh request"
  );

  private static final List<Severity> SEVERITIES = List.of(
    Severity.DEBUG,
    Severity.INFO,
    Severity.WARN,
    Severity.ERROR
  );

  private final Logger openTelemetryLogger;
  private final AtomicLong sequenceNumber;

  public RandomLogGeneratorService(OpenTelemetry openTelemetry) {
    this.openTelemetryLogger = openTelemetry.getLogsBridge()
      .loggerBuilder(RandomLogGeneratorService.class.getName())
      .build();
    this.sequenceNumber = new AtomicLong(0L);
  }

  @Scheduled(
    initialDelayString = "${app.log-generator.initial-delay-ms:1000}",
    fixedDelayString = "${app.log-generator.fixed-delay-ms:2000}"
  )
  public void emitRandomLog() {
    long currentSequence = sequenceNumber.incrementAndGet();
    String eventName = pickRandom(EVENTS);
    Severity severity = pickRandom(SEVERITIES);
    int simulatedLatencyMs = ThreadLocalRandom.current().nextInt(15, 450);
    boolean success = severity != Severity.ERROR;
    String message = "event=" + eventName + ", sequence=" + currentSequence + ", latencyMs=" + simulatedLatencyMs + ", success=" + success;

    APPLICATION_LOGGER.info("Generated test log: {}", message);

    openTelemetryLogger.logRecordBuilder()
      .setSeverity(severity)
      .setBody(message)
      .setAllAttributes(
        Attributes.builder()
          .put(AttributeKey.stringKey("app.event"), eventName)
          .put(AttributeKey.longKey("app.sequence"), currentSequence)
          .put(AttributeKey.longKey("app.latency_ms"), simulatedLatencyMs)
          .put(AttributeKey.booleanKey("app.success"), success)
          .build()
      )
      .emit();
  }

  private static <T> T pickRandom(List<T> values) {
    int randomIndex = ThreadLocalRandom.current().nextInt(values.size());
    return values.get(randomIndex);
  }
}
