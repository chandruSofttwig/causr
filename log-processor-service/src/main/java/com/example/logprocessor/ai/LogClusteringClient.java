package com.example.logprocessor.ai;

import com.example.logprocessor.config.AiProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class LogClusteringClient {

  private final RestTemplate http;
  private final AiProperties aiProperties;

  public LogClusteringClient(RestTemplateBuilder builder, AiProperties aiProperties) {
    this.http = builder.build();
    this.aiProperties = aiProperties;
  }

  public ClusterResponse cluster(String logLine) {
    if (!aiProperties.clusteringEnabled()) {
      return null;
    }
    try {
      URI uri = URI.create(aiProperties.httpBaseUrl().replaceAll("/$", "") + "/cluster");
      ResponseEntity<ClusterResponse> res =
          http.postForEntity(uri, new ClusterRequest(logLine != null ? logLine : ""), ClusterResponse.class);
      return res.getBody();
    } catch (RestClientException e) {
      return null;
    }
  }

  public record ClusterRequest(@JsonProperty("log_line") String logLine) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static final class ClusterResponse {
    @JsonProperty("cluster_id")
    public Integer clusterId;

    public String template;
  }
}
