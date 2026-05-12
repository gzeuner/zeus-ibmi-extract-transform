package de.zeus.ibmi.infrastructure.config;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum OutputFormat {
  XML("xml"),
  JSON("json"),
  JSONL("jsonl"),
  CSV("csv"),
  MD("md"),
  HTML("html");

  private final String id;

  OutputFormat(String id) {
    this.id = id;
  }

  public String id() {
    return id;
  }

  public static Optional<OutputFormat> from(String value) {
    if (value == null) {
      return Optional.empty();
    }
    String normalized = value.trim().toLowerCase(Locale.ROOT);
    return Arrays.stream(values()).filter(v -> v.id.equals(normalized)).findFirst();
  }
}
