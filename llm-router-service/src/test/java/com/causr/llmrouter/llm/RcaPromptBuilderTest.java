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

class GrokRcaClientTest {

  @Test
  void extractsOpenAiStyleMessageContent() {
    GrokRcaClient client =
        new GrokRcaClient(
            RestClient.builder(),
            new ObjectMapper(),
            "test-key",
            "https://api.x.ai/v1",
            "grok-4.6",
            256);
    String text =
        client.extractText(
            """
            {"choices":[{"message":{"role":"assistant","content":"Likely dependency timeout. Check payment DB."}}]}
            """);
    assertEquals("Likely dependency timeout. Check payment DB.", text);
  }
}
