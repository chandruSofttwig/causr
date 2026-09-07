package com.cursr.logsender.config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.resources.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenTelemetryConfig {

  @Bean(destroyMethod = "close")
  public OpenTelemetrySdk openTelemetrySdk(
    @Value("${otel.exporter.otlp.endpoint:http://localhost:4318}") String endpoint,
    @Value("${otel.exporter.otlp.protocol:http/protobuf}") String protocol,
    @Value("${otel.service.name:log-sender-backend}") String serviceName
  ) {
    LogRecordExporter logExporter = buildLogExporter(sanitizeEndpoint(endpoint), protocol);

    Resource resource = Resource.getDefault().merge(
      Resource.create(Attributes.of(AttributeKey.stringKey("service.name"), serviceName))
    );

    SdkLoggerProvider loggerProvider = SdkLoggerProvider.builder()
      .setResource(resource)
      .addLogRecordProcessor(BatchLogRecordProcessor.builder(logExporter).build())
      .build();

    return OpenTelemetrySdk.builder()
      .setLoggerProvider(loggerProvider)
      .build();
  }

  @Bean
  public OpenTelemetry openTelemetry(OpenTelemetrySdk openTelemetrySdk) {
    return openTelemetrySdk;
  }

  private static LogRecordExporter buildLogExporter(String endpoint, String protocol) {
    String normalized = protocol.trim().toLowerCase();
    if ("http/protobuf".equals(normalized) || "http_protobuf".equals(normalized)) {
      String httpEndpoint = endpoint.endsWith("/v1/logs") ? endpoint : endpoint + "/v1/logs";
      return OtlpHttpLogRecordExporter.builder()
        .setEndpoint(httpEndpoint)
        .build();
    }
    String grpcEndpoint = endpoint.endsWith("/v1/logs")
      ? endpoint.substring(0, endpoint.length() - "/v1/logs".length())
      : endpoint;
    return OtlpGrpcLogRecordExporter.builder()
      .setEndpoint(grpcEndpoint)
      .build();
  }

  private static String sanitizeEndpoint(String endpoint) {
    if (endpoint == null) {
      return "http://localhost:4318";
    }
    String sanitized = endpoint.trim();
    sanitized = sanitized.replace("\"", "");
    sanitized = sanitized.replace("\u0000", "");
    sanitized = sanitized.replace("\r", "");
    sanitized = sanitized.replace("\n", "");
    if (sanitized.isEmpty()) {
      return "http://localhost:4318";
    }
    return sanitized;
  }
}
