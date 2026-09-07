#!/usr/bin/env bash
set -euo pipefail

BOOTSTRAP="${KAFKA_BOOTSTRAP_SERVERS:-kafka:9092}"

echo "Creating Kafka topics on ${BOOTSTRAP}..."

create_topic() {
  local topic="$1"
  local partitions="$2"
  kafka-topics --bootstrap-server "${BOOTSTRAP}" \
    --create \
    --if-not-exists \
    --topic "${topic}" \
    --partitions "${partitions}" \
    --replication-factor 1
  echo "  OK: ${topic} (${partitions} partitions)"
}

create_topic logs.raw 3
create_topic traces.raw 3
create_topic anomaly-alerts 1
create_topic logs.anomalies 1
create_topic rca-ready 1

echo "Topics:"
kafka-topics --bootstrap-server "${BOOTSTRAP}" --list
