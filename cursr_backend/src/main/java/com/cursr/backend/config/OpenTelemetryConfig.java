package com.cursr.backend.config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.resources.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenTelemetryConfig {

  @Bean(destroyMethod = "close")
  public OpenTelemetrySdk openTelemetrySdk(
    @Value("${otel.exporter.otlp.endpoint:http://192.168.1.35:4317}") String endpoint,
    @Value("${otel.service.name:cursr-backend-log-tester}") String serviceName
  ) {
    OtlpGrpcLogRecordExporter logRecordExporter = OtlpGrpcLogRecordExporter.builder()
      .setEndpoint(endpoint)
      .build();

    Resource resource = Resource.getDefault().merge(
      Resource.create(
        Attributes.of(AttributeKey.stringKey("service.name"), serviceName)
      )
    );

    SdkLoggerProvider loggerProvider = SdkLoggerProvider.builder()
      .setResource(resource)
      .addLogRecordProcessor(BatchLogRecordProcessor.builder(logRecordExporter).build())
      .build();

    return OpenTelemetrySdk.builder()
      .setLoggerProvider(loggerProvider)
      .build();
  }

  @Bean
  public OpenTelemetry openTelemetry(OpenTelemetrySdk openTelemetrySdk) {
    return openTelemetrySdk;
  }
}
