package de.zeus.ibmi.infrastructure.config;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class OutputFormatParser {

  private OutputFormatParser() {}

  public static List<OutputFormat> parseCsv(String csv) {
    if (csv == null || csv.trim().isEmpty()) {
      return List.of(OutputFormat.XML, OutputFormat.JSON, OutputFormat.CSV, OutputFormat.MD);
    }
    String[] parts = csv.split(",");
    Set<OutputFormat> unique = new LinkedHashSet<>();
    for (String part : parts) {
      String normalized = part == null ? "" : part.trim();
      if (normalized.isEmpty()) {
        continue;
      }
      OutputFormat format =
          OutputFormat.from(normalized)
              .orElseThrow(
                  () -> new ConfigValidationException("Unsupported output format: " + normalized));
      unique.add(format);
    }
    if (unique.isEmpty()) {
      throw new ConfigValidationException("No valid output format was provided.");
    }
    return List.copyOf(new ArrayList<>(unique));
  }
}
