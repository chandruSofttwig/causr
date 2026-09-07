# log-sender-backend

Standalone Spring Boot application used only for log testing.

It continuously emits random **DEBUG**, **INFO**, **WARN**, and **ERROR** log records to OpenTelemetry (several per tick), with matching SLF4J mirror lines for local visibility.

## Requirements

- Java 21+
- Maven 3.9+

## Run

```bash
mvn spring-boot:run
```

Default endpoints:

- `http://localhost:8081/api/health`
- `http://localhost:8081/actuator/health`

## Config

Main config is in `src/main/resources/application.yml`.

- `otel.exporter.otlp.endpoint`: OTLP base URL (default `http://192.168.1.77:4318`)
- `otel.exporter.otlp.protocol`: `http/protobuf` (typical for port 4318) or `grpc` (use with port 4317). Mismatch causes `FRAME_SIZE_ERROR` when speaking gRPC to an HTTP port.
- `otel.service.name`: service name used in OpenTelemetry resource
- `app.log-loop.enabled`: turn random loop on/off
- `app.log-loop.initial-delay-ms`: first delay before loop starts
- `app.log-loop.fixed-delay-ms`: interval between scheduler ticks
- `app.log-loop.logs-per-tick`: how many random-severity OTLP log records each tick (default `6`)

## Test

```bash
mvn test
```
set OTEL_EXPORTER_OTLP_ENDPOINT=http://192.168.1.77:4317 && set OTEL_EXPORTER_OTLP_LOGS_ENDPOINT=http://192.168.1.77:4317 && java -javaagent:C:\otel\opentelemetry-javaagent.jar -Dotel.service.name=log-sender-backend -Dotel.resource.attributes=service.version=0.1.0,deployment.environment=dev -Dotel.exporter.otlp.endpoint=http://192.168.1.77:4317 -Dotel.exporter.otlp.protocol=grpc -Dotel.logs.exporter=otlp -Dotel.metrics.exporter=none -Dotel.traces.exporter=none -Dotel.instrumentation.logback-appender.enabled=true -jar D:\gitlab\cursr\log-sender-backend\target\log-sender-backend-0.1.0-SNAPSHOT.jar