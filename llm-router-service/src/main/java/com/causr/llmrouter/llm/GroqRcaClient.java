package com.causr.llmrouter.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Groq inference client (OpenAI-compatible Chat Completions).
 *
 * @see <a href="https://console.groq.com/docs/openai">Groq OpenAI compatibility</a>
 */
@Service
public class GroqRcaClient {

  private static final Logger log = LoggerFactory.getLogger(GroqRcaClient.class);

  private final RestClient restClient;
  private final ObjectMapper objectMapper;
  private final String apiKey;
  private final String model;
  private final int maxTokens;

  public GroqRcaClient(
      RestClient.Builder restClientBuilder,
      ObjectMapper objectMapper,
      @Value("${app.groq.api-key:}") String apiKey,
      @Value("${app.groq.base-url}") String baseUrl,
      @Value("${app.groq.model}") String model,
      @Value("${app.groq.max-tokens:512}") int maxTokens) {
    this.objectMapper = objectMapper;
    this.apiKey = apiKey == null ? "" : apiKey.trim();
    this.model = model;
    this.maxTokens = maxTokens;
    this.restClient = restClientBuilder.baseUrl(baseUrl.replaceAll("/$", "")).build();
  }

  public boolean isConfigured() {
    return !apiKey.isBlank();
  }

  public String getModel() {
    return model;
  }

  public String generateRca(String userPrompt) {
    if (!isConfigured()) {
      throw new IllegalStateException("GROQ_API_KEY is not set");
    }
    Map<String, Object> body =
        Map.of(
            "model",
            model,
            "max_tokens",
            maxTokens,
            "temperature",
            0.2,
            "messages",
            List.of(
                Map.of(
                    "role",
                    "system",
                    "content",
                    "You are an SRE assistant. Reply with plain RCA text only."),
                Map.of("role", "user", "content", userPrompt)));

    String response =
        restClient
            .post()
            .uri("/chat/completions")
            .contentType(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
            .body(body)
            .retrieve()
            .body(String.class);

    return extractText(response);
  }

  String extractText(String responseJson) {
    try {
      JsonNode root = objectMapper.readTree(responseJson);
      JsonNode choices = root.path("choices");
      if (choices.isArray() && !choices.isEmpty()) {
        JsonNode message = choices.get(0).path("message");
        JsonNode content = message.path("content");
        if (content.isTextual()) {
          return content.asText().trim();
        }
        if (content.isArray()) {
          StringBuilder sb = new StringBuilder();
          for (JsonNode part : content) {
            if (part.isTextual()) {
              if (!sb.isEmpty()) sb.append('\n');
              sb.append(part.asText());
            } else if (part.has("text")) {
              if (!sb.isEmpty()) sb.append('\n');
              sb.append(part.get("text").asText());
            }
          }
          if (!sb.isEmpty()) {
            return sb.toString().trim();
          }
        }
      }
      log.warn("Unexpected Groq response shape: {}", truncate(responseJson, 200));
      return responseJson == null ? "" : responseJson.trim();
    } catch (Exception e) {
      throw new IllegalStateException("Failed to parse Groq response: " + e.getMessage(), e);
    }
  }

  private static String truncate(String s, int max) {
    if (s == null) return "";
    return s.length() <= max ? s : s.substring(0, max) + "…";
  }
}
