package de.zeus.ibmi.infrastructure.output;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class OutputWriters {

  private OutputWriters() {}

  public static Map<String, OutputWriter> defaultWriters() {
    List<OutputWriter> writers = OutputWriterRegistry.defaultRegistry().writers();
    Map<String, OutputWriter> byFormat = new LinkedHashMap<>();
    for (OutputWriter writer : writers) {
      byFormat.put(writer.formatName(), writer);
    }
    return Map.copyOf(byFormat);
  }
}
