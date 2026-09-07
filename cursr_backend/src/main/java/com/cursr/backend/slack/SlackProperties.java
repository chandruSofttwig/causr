package com.cursr.backend.slack;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.slack")
public record SlackProperties(
    Boolean enabled,
    String webhookUrl,
    String dashboardBaseUrl,
    Integer dedupeWindowMinutes) {

  public SlackProperties {
    if (dashboardBaseUrl == null || dashboardBaseUrl.isBlank()) {
      dashboardBaseUrl = "http://localhost:5173";
    }
    if (dedupeWindowMinutes == null || dedupeWindowMinutes < 1) {
      dedupeWindowMinutes = 5;
    }
  }

  public boolean enabledOrDefault() {
    return enabled != null && enabled;
  }

  public boolean hasWebhook() {
    return webhookUrl != null && !webhookUrl.isBlank();
  }

  public String dashboardBaseUrlOrDefault() {
    return dashboardBaseUrl;
  }

  public int dedupeWindowMinutesOrDefault() {
    return dedupeWindowMinutes;
  }
}
