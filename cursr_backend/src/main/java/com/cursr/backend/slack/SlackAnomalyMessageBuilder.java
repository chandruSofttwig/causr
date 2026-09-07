package com.cursr.backend.slack;

import com.cursr.backend.anomaly.AnomalyAlertEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class SlackAnomalyMessageBuilder {

  private static final DateTimeFormatter WINDOW_FMT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);

  private final ObjectMapper objectMapper;

  public SlackAnomalyMessageBuilder(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public Map<String, Object> buildBlocks(AnomalyAlertEvent event, String dashboardBaseUrl) {
    String service = nullToDash(event.serviceName());
    String env = nullToDash(event.environment());
    String tenant = nullToDash(event.tenantId());
    String windowStart = formatEpoch(event.windowStartEpochMs());
    String windowEnd = formatEpoch(event.windowEndEpochMs());
    String anomalyId = event.id() != null ? event.id().toString() : "—";
    String dashboardUrl = trimTrailingSlash(dashboardBaseUrl) + "/anomalies";

    List<Map<String, Object>> blocks = new ArrayList<>();

    blocks.add(
        Map.of(
            "type",
            "header",
            "text",
            Map.of(
                "type",
                "plain_text",
                "text",
                "Anomaly detected — " + service + " (" + env + ")",
                "emoji",
                true)));

    List<Map<String, Object>> fields = new ArrayList<>();
    fields.add(fieldBlock("*Score*\n" + event.anomalyScore()));
    fields.add(fieldBlock("*Tenant*\n" + tenant));
    fields.add(fieldBlock("*Window start*\n" + windowStart));
    fields.add(fieldBlock("*Window end*\n" + windowEnd));

    String errorRate = extractErrorRate(event.featureJson());
    if (errorRate != null) {
      fields.add(fieldBlock("*Error rate*\n" + errorRate));
    }

    blocks.add(Map.of("type", "section", "fields", fields));
    blocks.add(
        Map.of(
            "type",
            "section",
            "text",
            Map.of(
                "type",
                "mrkdwn",
                "text",
                "<" + dashboardUrl + "|Open anomalies dashboard>")));
    blocks.add(
        Map.of(
            "type",
            "context",
            "elements",
            List.of(
                Map.of(
                    "type",
                    "mrkdwn",
                    "text",
                    "Anomaly id: `" + anomalyId + "`"))));

    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("blocks", blocks);
    return payload;
  }

  private static Map<String, Object> fieldBlock(String mrkdwn) {
    return Map.of("type", "mrkdwn", "text", mrkdwn);
  }

  private String extractErrorRate(String featureJson) {
    if (featureJson == null || featureJson.isBlank()) {
      return null;
    }
    try {
      JsonNode node = objectMapper.readTree(featureJson);
      JsonNode rate = node.get("error_rate");
      if (rate != null && rate.isNumber()) {
        return String.format("%.2f%%", rate.asDouble() * 100);
      }
    } catch (Exception ignored) {
      // optional field
    }
    return null;
  }

  private static String formatEpoch(long epochMs) {
    if (epochMs <= 0) {
      return "—";
    }
    return WINDOW_FMT.format(Instant.ofEpochMilli(epochMs));
  }

  private static String nullToDash(String value) {
    return value == null || value.isBlank() ? "—" : value;
  }

  private static String trimTrailingSlash(String url) {
    if (url == null || url.isBlank()) {
      return "http://localhost:5173";
    }
    return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
  }
}
