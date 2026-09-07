package com.cursr.backend.slack;

import com.cursr.backend.anomaly.RcaReadyEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class SlackRcaMessageBuilder {

  public Map<String, Object> buildBlocks(RcaReadyEvent event, String dashboardBaseUrl) {
    String service = nullToDash(event.serviceName());
    String env = nullToDash(event.environment());
    String rca = event.rcaText() == null ? "" : event.rcaText().trim();
    if (rca.length() > 2800) {
      rca = rca.substring(0, 2800) + "…";
    }
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
                "RCA ready — " + service + " (" + env + ")",
                "emoji",
                true)));
    blocks.add(
        Map.of(
            "type",
            "section",
            "text",
            Map.of("type", "mrkdwn", "text", rca.isBlank() ? "_(empty RCA)_" : rca)));
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

    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("blocks", blocks);
    return payload;
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
