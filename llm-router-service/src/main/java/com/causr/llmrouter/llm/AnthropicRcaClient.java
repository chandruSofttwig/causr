package com.causr.llmrouter.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class AnthropicRcaClient {

  private static final Logger log = LoggerFactory.getLogger(AnthropicRcaClient.class);

  private final RestClient restClient;
  private final ObjectMapper objectMapper;
  private final String apiKey;
  private final String model;
  private final int maxTokens;

  public AnthropicRcaClient(
      RestClient.Builder restClientBuilder,
      ObjectMapper objectMapper,
      @Value("${app.anthropic.api-key:}") String apiKey,
      @Value("${app.anthropic.base-url}") String baseUrl,
      @Value("${app.anthropic.model}") String model,
      @Value("${app.anthropic.max-tokens:512}") int maxTokens) {
    this.objectMapper = objectMapper;
    this.apiKey = apiKey == null ? "" : apiKey.trim();
    this.model = model;
    this.maxTokens = maxTokens;
    this.restClient = restClientBuilder.baseUrl(baseUrl.replaceAll("/$", "")).build();
  }

  public boolean isConfigured() {
    return !apiKey.isBlank();
  }

  public String generateRca(String userPrompt) {
    if (!isConfigured()) {
      throw new IllegalStateException("ANTHROPIC_API_KEY is not set");
    }
    Map<String, Object> body =
        Map.of(
            "model",
            model,
            "max_tokens",
            maxTokens,
            "messages",
            List.of(Map.of("role", "user", "content", userPrompt)));

    String response =
        restClient
            .post()
            .uri("/v1/messages")
            .contentType(MediaType.APPLICATION_JSON)
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .body(body)
            .retrieve()
            .body(String.class);

    return extractText(response);
  }

  String extractText(String responseJson) {
    try {
      JsonNode root = objectMapper.readTree(responseJson);
      JsonNode content = root.path("content");
      if (content.isArray()) {
        StringBuilder sb = new StringBuilder();
        for (JsonNode block : content) {
          if ("text".equals(block.path("type").asText()) && block.has("text")) {
            if (!sb.isEmpty()) sb.append('\n');
            sb.append(block.get("text").asText());
          }
        }
        if (!sb.isEmpty()) {
          return sb.toString().trim();
        }
      }
      log.warn("Unexpected Anthropic response shape: {}", truncate(responseJson, 200));
      return responseJson == null ? "" : responseJson.trim();
    } catch (Exception e) {
      throw new IllegalStateException("Failed to parse Anthropic response: " + e.getMessage(), e);
    }
  }

  private static String truncate(String s, int max) {
    if (s == null) return "";
    return s.length() <= max ? s : s.substring(0, max) + "…";
  }
}
