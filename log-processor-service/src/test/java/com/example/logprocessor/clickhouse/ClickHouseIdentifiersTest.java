package com.example.logprocessor.clickhouse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ClickHouseIdentifiersTest {

  @Test
  void usesDefaultWhenBlank() {
    assertEquals("logs_hot", ClickHouseIdentifiers.resolveTableName(null, "logs_hot"));
    assertEquals("logs_hot", ClickHouseIdentifiers.resolveTableName("  ", "logs_hot"));
  }

  @Test
  void acceptsSimpleIdentifier() {
    assertEquals("logs_cold", ClickHouseIdentifiers.resolveTableName("logs_cold", "logs_hot"));
  }

  @Test
  void acceptsQualifiedDbTable() {
    assertEquals("observability.logs_cold", ClickHouseIdentifiers.resolveTableName("observability.logs_cold", "x"));
  }

  @Test
  void rejectsInjectionLikeCharacters() {
    assertThrows(
        IllegalArgumentException.class,
        () -> ClickHouseIdentifiers.resolveTableName("logs;drop", "logs_hot"));
    assertThrows(
        IllegalArgumentException.class,
        () -> ClickHouseIdentifiers.resolveTableName("logs hot", "logs_hot"));
  }
}
