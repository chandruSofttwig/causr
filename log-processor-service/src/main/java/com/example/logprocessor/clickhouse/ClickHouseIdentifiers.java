package com.example.logprocessor.clickhouse;

import java.util.regex.Pattern;

/**
 * Validates ClickHouse table identifiers so dynamically chosen names cannot break out of the
 * identifier position in SQL (defense in depth when names come from configuration).
 */
public final class ClickHouseIdentifiers {

  private static final Pattern SAFE_TABLE =
      Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*(?:\\.[A-Za-z_][A-Za-z0-9_]*)?$");

  private ClickHouseIdentifiers() {}

  /**
   * Returns {@code configured} trimmed if non-blank, else {@code defaultName}. Both must match a
   * conservative safe pattern (optionally {@code database.table} with one dot).
   *
   * @throws IllegalArgumentException if the resolved name is not safe to embed as an identifier
   */
  public static String resolveTableName(String configured, String defaultName) {
    String name =
        configured != null && !configured.isBlank() ? configured.trim() : defaultName;
    if (!SAFE_TABLE.matcher(name).matches()) {
      throw new IllegalArgumentException(
          "Invalid ClickHouse table identifier (use letters, digits, underscore; optional db.table): "
              + name);
    }
    return name;
  }
}
