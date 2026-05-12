package de.zeus.ibmi.infrastructure.output;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class OutputWriterRegistry {

  private final Map<String, OutputWriter> writersByFormat;

  public OutputWriterRegistry(List<OutputWriter> writers) {
    Map<String, OutputWriter> byFormat = new LinkedHashMap<>();
    for (OutputWriter writer : writers) {
      byFormat.put(writer.formatName(), writer);
    }
    this.writersByFormat = Map.copyOf(byFormat);
  }

  public OutputWriter writerFor(String format) {
    OutputWriter writer = writersByFormat.get(format);
    if (writer == null) {
      throw new IllegalArgumentException("Unsupported output format: " + format);
    }
    return writer;
  }

  public List<OutputWriter> writers() {
    return List.copyOf(writersByFormat.values());
  }

  public static OutputWriterRegistry defaultRegistry() {
    return new OutputWriterRegistry(
        List.of(
            new XmlOutputWriter(),
            new JsonOutputWriter(),
            new JsonlOutputWriter(),
            new CsvOutputWriter(),
            new MarkdownOutputWriter(),
            new HtmlOutputWriter()));
  }
}
