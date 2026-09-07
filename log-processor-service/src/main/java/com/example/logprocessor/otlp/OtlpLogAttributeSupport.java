package com.example.logprocessor.otlp;

import java.util.Map;

/**
 * Shared OTLP log attribute parsing for Kafka ingest and Kafka Streams so ClickHouse
 * {@code duration_ms} and dashboard SQL ({@code http.status_code}) stay consistent.
 */
public final class OtlpLogAttributeSupport {

  /** Keys whose values are interpreted as milliseconds (double or int string). */
  private static final String[] DURATION_MS_ATTRIBUTE_KEYS = {
    "duration_ms",
    "app.latency_ms",
    "http.server.duration",
    "http.client.duration",
  };

  /**
   * OpenTelemetry semantic conventions: request duration in <strong>seconds</strong> (e.g. stable
   * {@code http.server.request.duration}).
   */
  private static final String[] DURATION_SECONDS_ATTRIBUTE_KEYS = {
    "http.server.request.duration",
    "http.client.request.duration",
  };

  private static final long MAX_DURATION_MS = 86_400_000L;

  private OtlpLogAttributeSupport() {}

  /**
   * If {@code http.status_code} is missing and {@code http.response.status_code} is set, copies
   * the latter so backend queries using {@code attributes['http.status_code']} still match.
   */
  public static void normalizeHttpStatusCodeAlias(Map<String, String> attrs) {
    if (attrs == null) {
      return;
    }
    String legacy = attrs.get("http.status_code");
    if (legacy != null && !legacy.isBlank()) {
      return;
    }
    String v = attrs.get("http.response.status_code");
    if (v != null && !v.isBlank()) {
      attrs.put("http.status_code", v.trim());
    }
  }

  /**
   * First non-empty attribute among {@link #DURATION_MS_ATTRIBUTE_KEYS} (parsed as ms), then among
   * {@link #DURATION_SECONDS_ATTRIBUTE_KEYS} (parsed as seconds → ms). Returns 0 if none found or
   * invalid.
   */
  public static long latencyMsFromAttributes(Map<String, String> attrs) {
    if (attrs == null || attrs.isEmpty()) {
      return 0L;
    }
    for (String key : DURATION_MS_ATTRIBUTE_KEYS) {
      long ms = parseMilliseconds(attrs.get(key));
      if (ms > 0) {
        return Math.min(ms, MAX_DURATION_MS);
      }
    }
    for (String key : DURATION_SECONDS_ATTRIBUTE_KEYS) {
      long ms = secondsToBoundedMs(attrs.get(key));
      if (ms > 0) {
        return ms;
      }
    }
    return 0L;
  }

  /** Non-null bounded positive int for {@link com.example.logprocessor.model.RawLogEvent#duration_ms}. */
  public static Integer toNullableDurationMsInt(long latencyMs) {
    if (latencyMs <= 0) {
      return null;
    }
    long capped = Math.min(latencyMs, MAX_DURATION_MS);
    if (capped > Integer.MAX_VALUE) {
      return Integer.MAX_VALUE;
    }
    return (int) capped;
  }

  static long parseMilliseconds(String raw) {
    if (raw == null || raw.isBlank()) {
      return 0L;
    }
    try {
      double d = Double.parseDouble(raw.trim());
      long rounded = Math.round(d);
      return rounded > 0 ? Math.min(rounded, MAX_DURATION_MS) : 0L;
    } catch (NumberFormatException e) {
      return 0L;
    }
  }

  private static long secondsToBoundedMs(String raw) {
    if (raw == null || raw.isBlank()) {
      return 0L;
    }
    try {
      double seconds = Double.parseDouble(raw.trim());
      if (seconds <= 0 || Double.isNaN(seconds) || Double.isInfinite(seconds)) {
        return 0L;
      }
      double ms = seconds * 1000.0;
      if (ms > MAX_DURATION_MS) {
        return MAX_DURATION_MS;
      }
      long rounded = Math.round(ms);
      return rounded > 0 ? rounded : 0L;
    } catch (NumberFormatException e) {
      return 0L;
    }
  }
}
