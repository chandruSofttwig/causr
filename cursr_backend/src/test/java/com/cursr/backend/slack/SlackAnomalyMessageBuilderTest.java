package com.cursr.backend.slack;

import static org.assertj.core.api.Assertions.assertThat;

import com.cursr.backend.anomaly.AnomalyAlertEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SlackAnomalyMessageBuilderTest {

  private final SlackAnomalyMessageBuilder builder =
      new SlackAnomalyMessageBuilder(new ObjectMapper());

  @Test
  void buildBlocks_includesServiceEnvironmentScoreAndDashboardLink() {
    UUID id = UUID.fromString("11111111-1111-1111-1111-111111111111");
    AnomalyAlertEvent event =
        new AnomalyAlertEvent(
            id,
            1_700_000_000_000L,
            1_700_000_030_000L,
            "tenant-1",
            "payment-service",
            "staging",
            -0.55f,
            "{\"error_rate\":0.42}");

    Map<String, Object> payload = builder.buildBlocks(event, "http://localhost:5173");

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> blocks = (List<Map<String, Object>>) payload.get("blocks");
    assertThat(blocks).hasSizeGreaterThanOrEqualTo(3);

    Map<String, Object> header = blocks.get(0);
    assertThat(header.get("type")).isEqualTo("header");
    @SuppressWarnings("unchecked")
    Map<String, Object> headerText = (Map<String, Object>) header.get("text");
    assertThat(String.valueOf(headerText.get("text")))
        .contains("payment-service")
        .contains("staging");

    String json = new ObjectMapper().valueToTree(payload).toString();
    assertThat(json).contains("payment-service");
    assertThat(json).contains("http://localhost:5173/anomalies");
    assertThat(json).contains("11111111-1111-1111-1111-111111111111");
    assertThat(json).contains("42.00%");
  }
}
