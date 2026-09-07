package com.cursr.logsender.logging;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.Severity;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(
  prefix = "app.log-loop",
  name = "enabled",
  havingValue = "true",
  matchIfMissing = true
)
public class RandomLogLoopService {

  private static final org.slf4j.Logger APP_LOGGER = LoggerFactory.getLogger(RandomLogLoopService.class);

  private static final List<String> SERVICES = List.of(
    "api-gateway",
    "auth-service",
    "checkout-service",
    "inventory-service",
    "payment-service",
    "notification-service"
  );

  private static final List<String> SPAN_KINDS = List.of(
    "SERVER",
    "CLIENT",
    "INTERNAL"
  );

  private final Logger openTelemetryLogger;
  private final AtomicLong messageCounter;
  private final int logsPerTick;

  public RandomLogLoopService(
    OpenTelemetry openTelemetry,
    @Value("${app.log-loop.logs-per-tick:6}") int logsPerTick
  ) {
    this.openTelemetryLogger = openTelemetry.getLogsBridge()
      .loggerBuilder(RandomLogLoopService.class.getName())
      .build();
    this.messageCounter = new AtomicLong(0L);
    this.logsPerTick = Math.max(1, logsPerTick);
  }

  @Scheduled(
    initialDelayString = "${app.log-loop.initial-delay-ms:500}",
    fixedDelayString = "${app.log-loop.fixed-delay-ms:1000}"
  )
  public void publishRandomLogs() {
    for (int i = 0; i < logsPerTick; i++) {
      emitOneRandomOtelLog();
    }
  }

  private void emitOneRandomOtelLog() {
    long currentCounter = messageCounter.incrementAndGet();
    IssueScenario scenario = pickRandom(issueScenarios());
    String serviceName = pickRandom(SERVICES);
    String spanKind = pickRandom(SPAN_KINDS);
    int latencyMs = ThreadLocalRandom.current().nextInt(15, 1200);
    boolean success = scenario.severity != Severity.ERROR;
    String traceId = randomHex(32);
    String spanId = randomHex(16);
    String parentSpanId = randomHex(16);
    String environment = pickRandom(List.of("prod", "staging"));

    String severityText = severityToText(scenario.severity);
    String payload = "traceId=" + traceId
      + ", spanId=" + spanId
      + ", parentSpanId=" + parentSpanId
      + ", service=" + serviceName
      + ", env=" + environment
      + ", spanKind=" + spanKind
      + ", operation=" + scenario.operation
      + ", issueType=" + scenario.issueType
      + ", statusCode=" + scenario.httpStatus
      + ", latencyMs=" + latencyMs
      + ", success=" + success
      + ", counter=" + currentCounter
      + ", message=" + scenario.message;
    logSlf4jMirror(scenario.severity, payload);

    openTelemetryLogger.logRecordBuilder()
      .setSeverity(scenario.severity)
      .setBody(payload)
      .setAllAttributes(
        Attributes.builder()
          .put(AttributeKey.stringKey("trace.id"), traceId)
          .put(AttributeKey.stringKey("span.id"), spanId)
          .put(AttributeKey.stringKey("parent.span.id"), parentSpanId)
          .put(AttributeKey.stringKey("service.name"), serviceName)
          .put(AttributeKey.stringKey("tenant.id"), "default")
          .put(AttributeKey.stringKey("deployment.environment"), environment)
          .put(AttributeKey.stringKey("span.kind"), spanKind)
          .put(AttributeKey.stringKey("operation.name"), scenario.operation)
          .put(AttributeKey.stringKey("issue.type"), scenario.issueType)
          .put(AttributeKey.stringKey("exception.type"), scenario.exceptionType)
          .put(AttributeKey.stringKey("exception.message"), scenario.message)
          .put(AttributeKey.longKey("http.status_code"), scenario.httpStatus)
          .put(AttributeKey.stringKey("app.severity"), severityText)
          .put(AttributeKey.stringKey("app.event"), scenario.operation)
          .put(AttributeKey.longKey("app.counter"), currentCounter)
          .put(AttributeKey.longKey("app.latency_ms"), latencyMs)
          .put(AttributeKey.booleanKey("app.success"), success)
          .build()
      )
      .emit();
  }

  private static List<IssueScenario> issueScenarios() {
    List<IssueScenario> items = new ArrayList<>();
    items.add(new IssueScenario(Severity.INFO, "Login successful", "Auth token validated", "auth.login", "normal", 200, "none"));
    items.add(new IssueScenario(Severity.WARN, "Slow checkout dependency", "Checkout dependency latency elevated", "checkout.place_order", "latency", 200, "none"));
    items.add(new IssueScenario(Severity.ERROR, "Payment timeout", "Upstream payment provider timeout", "payment.authorize", "dependency_timeout", 504, "java.net.SocketTimeoutException"));
    items.add(new IssueScenario(Severity.ERROR, "Inventory deadlock", "Inventory lock acquisition failed", "inventory.reserve", "db_deadlock", 500, "org.postgresql.util.PSQLException"));
    items.add(new IssueScenario(Severity.ERROR, "Kafka publish failure", "Event publish failed after retries", "notification.publish", "event_delivery_failure", 502, "org.apache.kafka.common.KafkaException"));
    items.add(new IssueScenario(Severity.WARN, "Circuit breaker open", "Payment circuit breaker opened", "payment.authorize", "circuit_open", 503, "none"));
    items.add(new IssueScenario(Severity.DEBUG, "Cache warmup", "Inventory cache refresh completed", "inventory.cache_refresh", "diagnostic", 200, "none"));
    return items;
  }

  private static String randomHex(int length) {
    char[] chars = new char[length];
    ThreadLocalRandom random = ThreadLocalRandom.current();
    for (int i = 0; i < length; i++) {
      int value = random.nextInt(16);
      chars[i] = Character.forDigit(value, 16);
    }
    return new String(chars).toLowerCase(Locale.ROOT);
  }

  private static void logSlf4jMirror(Severity severity, String payload) {
    if (severity == Severity.ERROR) {
      APP_LOGGER.error("[otel-sample] {}", payload);
    } else if (severity == Severity.WARN) {
      APP_LOGGER.warn("[otel-sample] {}", payload);
    } else if (severity == Severity.DEBUG) {
      APP_LOGGER.debug("[otel-sample] {}", payload);
    } else {
      APP_LOGGER.info("[otel-sample] {}", payload);
    }
  }

  private static String severityToText(Severity severity) {
    if (severity == Severity.DEBUG) {
      return "DEBUG";
    }
    if (severity == Severity.INFO) {
      return "INFO";
    }
    if (severity == Severity.WARN) {
      return "WARN";
    }
    if (severity == Severity.ERROR) {
      return "ERROR";
    }
    return severity.name();
  }

  private static <T> T pickRandom(List<T> options) {
    int index = ThreadLocalRandom.current().nextInt(options.size());
    return options.get(index);
  }

  private record IssueScenario(
    Severity severity,
    String message,
    String detail,
    String operation,
    String issueType,
    long httpStatus,
    String exceptionType
  ) {
  }
}
