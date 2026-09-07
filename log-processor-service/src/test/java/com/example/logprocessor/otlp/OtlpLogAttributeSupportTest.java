package com.example.logprocessor.otlp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OtlpLogAttributeSupportTest {

  @Test
  void latencyPrefersDurationMsOverServerDuration() {
    Map<String, String> m = new HashMap<>();
    m.put("http.server.duration", "99");
    m.put("duration_ms", "42");
    assertEquals(42L, OtlpLogAttributeSupport.latencyMsFromAttributes(m));
  }

  @Test
  void latencyFallsBackToHttpServerDuration() {
    Map<String, String> m = Map.of("http.server.duration", "150.4");
    assertEquals(150L, OtlpLogAttributeSupport.latencyMsFromAttributes(m));
  }

  @Test
  void latencyUsesHttpClientDuration() {
    Map<String, String> m = Map.of("http.client.duration", "80");
    assertEquals(80L, OtlpLogAttributeSupport.latencyMsFromAttributes(m));
  }

  @Test
  void latencyParsesServerRequestDurationInSeconds() {
    Map<String, String> m = Map.of("http.server.request.duration", "0.125");
    assertEquals(125L, OtlpLogAttributeSupport.latencyMsFromAttributes(m));
  }

  @Test
  void latencyMsKeysWinOverSecondsKeys() {
    Map<String, String> m = new HashMap<>();
    m.put("http.server.request.duration", "10");
    m.put("http.server.duration", "5");
    assertEquals(5L, OtlpLogAttributeSupport.latencyMsFromAttributes(m));
  }

  @Test
  void normalizeCopiesResponseStatusWhenLegacyMissing() {
    Map<String, String> m = new HashMap<>();
    m.put("http.response.status_code", "503");
    OtlpLogAttributeSupport.normalizeHttpStatusCodeAlias(m);
    assertEquals("503", m.get("http.status_code"));
    assertEquals("503", m.get("http.response.status_code"));
  }

  @Test
  void normalizeDoesNotOverwriteExistingHttpStatusCode() {
    Map<String, String> m = new HashMap<>();
    m.put("http.status_code", "200");
    m.put("http.response.status_code", "500");
    OtlpLogAttributeSupport.normalizeHttpStatusCodeAlias(m);
    assertEquals("200", m.get("http.status_code"));
  }

  @Test
  void toNullableDurationMsIntReturnsNullForZero() {
    assertNull(OtlpLogAttributeSupport.toNullableDurationMsInt(0L));
  }

  @Test
  void parseMillisecondsTrimsAndRounds() {
    assertEquals(0L, OtlpLogAttributeSupport.parseMilliseconds(""));
    assertEquals(0L, OtlpLogAttributeSupport.parseMilliseconds("abc"));
    assertEquals(3L, OtlpLogAttributeSupport.parseMilliseconds(" 2.6 "));
  }
}
