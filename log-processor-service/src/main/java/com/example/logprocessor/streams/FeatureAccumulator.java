package com.example.logprocessor.streams;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tdunning.math.stats.MergingDigest;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

/**
 * Mutable aggregate for Kafka Streams windows; serialized as JSON for state store.
 */
public final class FeatureAccumulator {

  public long totalCount;
  public long errorCount;
  public String tenantId = "";
  public Set<String> errorTemplates = new HashSet<>();
  private String digestBase64 = "";

  public void accumulate(StreamLogEvent e) {
    if (tenantId.isEmpty() && e.tenantId() != null && !e.tenantId().isBlank()) {
      tenantId = e.tenantId();
    }
    totalCount++;
    if (e.error()) {
      errorCount++;
      errorTemplates.add(e.templateHash());
    }
    if (e.latencyMs() > 0) {
      MergingDigest digest = digest();
      digest.add(e.latencyMs());
      java.nio.ByteBuffer buf = java.nio.ByteBuffer.allocate(digest.smallByteSize());
      digest.asSmallBytes(buf);
      buf.flip();
      byte[] arr = new byte[buf.remaining()];
      buf.get(arr);
      digestBase64 = Base64.getEncoder().encodeToString(arr);
    }
  }

  @JsonIgnore
  public MergingDigest digest() {
    if (digestBase64 == null || digestBase64.isEmpty()) {
      return new MergingDigest(100);
    }
    byte[] bytes = Base64.getDecoder().decode(digestBase64);
    return MergingDigest.fromBytes(ByteBuffer.wrap(bytes));
  }

  public String getDigestBase64() {
    return digestBase64;
  }

  public void setDigestBase64(String digestBase64) {
    this.digestBase64 = digestBase64 != null ? digestBase64 : "";
  }
}
