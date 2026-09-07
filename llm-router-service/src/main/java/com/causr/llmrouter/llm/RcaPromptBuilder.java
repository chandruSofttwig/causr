package com.causr.llmrouter.llm;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class RcaPromptBuilder {

  public String buildUserPrompt(
      String serviceName,
      String environment,
      float anomalyScore,
      String featureJson,
      List<Map<String, Object>> errorLogs) {
    StringBuilder sb = new StringBuilder();
    sb.append("You are an SRE assistant. Write a concise root-cause analysis (RCA) for an anomaly.\n");
    sb.append("Return plain text only: 3-6 short sentences covering likely cause, impact, and next checks.\n\n");
    sb.append("Service: ").append(nullToDash(serviceName)).append('\n');
    sb.append("Environment: ").append(nullToDash(environment)).append('\n');
    sb.append("Anomaly score: ").append(anomalyScore).append('\n');
    sb.append("Features JSON: ").append(featureJson == null || featureJson.isBlank() ? "{}" : featureJson);
    sb.append("\n\nRecent related logs:\n");
    if (errorLogs == null || errorLogs.isEmpty()) {
      sb.append("(no recent ERROR/WARN logs found in the anomaly window)\n");
    } else {
      String lines =
          errorLogs.stream()
              .map(
                  row ->
                      String.format(
                          "- [%s] %s | %s",
                          String.valueOf(row.getOrDefault("timestamp", "")),
                          String.valueOf(row.getOrDefault("log_level", "")),
                          truncate(String.valueOf(row.getOrDefault("message", "")), 240)))
              .collect(Collectors.joining("\n"));
      sb.append(lines).append('\n');
    }
    return sb.toString();
  }

  private static String nullToDash(String v) {
    return v == null || v.isBlank() ? "—" : v;
  }

  private static String truncate(String s, int max) {
    if (s == null) return "";
    return s.length() <= max ? s : s.substring(0, max) + "…";
  }
}
