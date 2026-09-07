package com.example.logprocessor.streams;

import java.time.Duration;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Suppressed;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.kstream.Windowed;
import org.apache.kafka.streams.state.WindowStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.support.serializer.JsonSerde;

@Configuration
@EnableKafkaStreams
@ConditionalOnProperty(name = "app.streams.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaStreamsTopologyConfiguration {

  @Bean
  public JsonSerde<StreamLogEvent> streamLogSerde() {
    return new JsonSerde<>(StreamLogEvent.class);
  }

  @Bean
  public JsonSerde<FeatureAccumulator> featureAccumulatorSerde() {
    return new JsonSerde<>(FeatureAccumulator.class);
  }

  @Bean
  public KStream<Windowed<String>, FeatureAccumulator> serviceMetricsWindows(
      StreamsBuilder builder,
      JsonSerde<StreamLogEvent> streamLogSerde,
      JsonSerde<FeatureAccumulator> accSerde,
      ServiceMetricsWindowHandler handler) {

    KStream<byte[], byte[]> raw =
        builder.stream(
            "logs.raw", Consumed.with(Serdes.ByteArray(), Serdes.ByteArray()));

    KStream<String, StreamLogEvent> events =
        raw.flatMap((k, v) -> OtlpStreamEventExtractor.toKeyedEvents(v));

    TimeWindows windows = TimeWindows.ofSizeWithNoGrace(Duration.ofSeconds(30));

    KTable<Windowed<String>, FeatureAccumulator> windowed =
        events
            .groupByKey(Grouped.with(Serdes.String(), streamLogSerde))
            .windowedBy(windows)
            .aggregate(
                FeatureAccumulator::new,
                (key, value, aggregate) -> {
                  aggregate.accumulate(value);
                  return aggregate;
                },
                Materialized.<String, FeatureAccumulator, WindowStore<Bytes, byte[]>>as(
                        "service-metrics-store")
                    .withKeySerde(Serdes.String())
                    .withValueSerde(accSerde));

    KStream<Windowed<String>, FeatureAccumulator> out =
        windowed
            .suppress(Suppressed.untilWindowCloses(Suppressed.BufferConfig.unbounded()))
            .toStream();
    out.foreach(handler::handle);
    return out;
  }
}
