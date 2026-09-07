package com.example.logprocessor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.streams")
public record StreamsAppProperties(Boolean enabled) {

  public boolean enabledOrDefault() {
    return enabled == null || enabled;
  }
}
