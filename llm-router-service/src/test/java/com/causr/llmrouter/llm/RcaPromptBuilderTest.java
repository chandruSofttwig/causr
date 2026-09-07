package com.causr.llmrouter.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class RcaPromptBuilderTest {

  @Test
  void includesServiceAndLogLines() {
    RcaPromptBuilder builder = new RcaPromptBuilder();
    String prompt =
        builder.buildUserPrompt(
            "payment-service",
            "staging",
            -0.55f,
            "{\"error_rate\":0.4}",
            List.of(Map.of("timestamp", "2026-01-01T00:00:00Z", "log_level", "ERROR", "message", "timeout")));
    assertTrue(prompt.contains("payment-service"));
    assertTrue(prompt.contains("timeout"));
    assertTrue(prompt.contains("error_rate"));
  }
}

class AnthropicRcaClientTest {

  @Test
  void extractsTextBlocks() {
    AnthropicRcaClient client =
        new AnthropicRcaClient(
            RestClient.builder(),
            new ObjectMapper(),
            "test-key",
            "https://api.anthropic.com",
            "claude-3-5-haiku-20241022",
            256);
    String text =
        client.extractText(
            """
            {"content":[{"type":"text","text":"Likely dependency timeout."},{"type":"text","text":"Check payment DB."}]}
            """);
    assertEquals("Likely dependency timeout.\nCheck payment DB.", text);
  }
}
