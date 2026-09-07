package com.example.logprocessor.ai;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.example.logprocessor.config.AiProperties;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.annotation.PreDestroy;

@Component
public class GrpcAnomalyScorerClient {

  private static final Logger log = LoggerFactory.getLogger(GrpcAnomalyScorerClient.class);

  private final ManagedChannel channel;
  private final AnomalyScorerGrpc.AnomalyScorerBlockingStub stub;

  public GrpcAnomalyScorerClient(AiProperties aiProperties) {
    String target = aiProperties.grpcTarget().trim();
    int colon = target.lastIndexOf(':');
    String host = colon > 0 ? target.substring(0, colon) : target;
    int port = colon > 0 ? Integer.parseInt(target.substring(colon + 1)) : 50051;
    this.channel =
        ManagedChannelBuilder.forAddress(host, port)
            .usePlaintext()
            .build();
    this.stub = AnomalyScorerGrpc.newBlockingStub(channel);
  }

  public Optional<ScoreBatchResponse> scoreBatch(List<FeatureVector> vectors) {
    if (vectors.isEmpty()) {
      return Optional.of(ScoreBatchResponse.getDefaultInstance());
    }
    try {
      ScoreBatchRequest req = ScoreBatchRequest.newBuilder().addAllVectors(vectors).build();
      ScoreBatchResponse res = stub.withDeadlineAfter(5, TimeUnit.SECONDS).scoreBatch(req);
      return Optional.of(res);
    } catch (Exception e) {
      log.warn("AI gRPC scoreBatch failed ({} vectors): {}", vectors.size(), e.getMessage());
      return Optional.empty();
    }
  }

  public Optional<ScoreResult> scoreOne(FeatureVector vector) {
    return scoreBatch(List.of(vector))
        .filter(r -> r.getResultsCount() > 0)
        .map(r -> r.getResults(0));
  }

  @PreDestroy
  public void shutdown() throws InterruptedException {
    channel.shutdown();
    channel.awaitTermination(3, TimeUnit.SECONDS);
  }
}
