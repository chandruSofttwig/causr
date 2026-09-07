package com.cursr.backend.slack;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class SlackWebhookClient {

  private static final Logger log = LoggerFactory.getLogger(SlackWebhookClient.class);

  private final RestClient restClient;

  public SlackWebhookClient() {
    this.restClient = RestClient.create();
  }

  SlackWebhookClient(RestClient restClient) {
    this.restClient = restClient;
  }

  public void post(String webhookUrl, Map<String, Object> payload) {
    try {
      restClient
          .post()
          .uri(webhookUrl)
          .contentType(MediaType.APPLICATION_JSON)
          .body(payload)
          .retrieve()
          .toBodilessEntity();
    } catch (RestClientException e) {
      log.warn("Slack webhook POST failed: {}", e.getMessage());
      throw e;
    }
  }
}
