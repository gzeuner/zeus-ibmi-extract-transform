package de.zeus.ibmi.output;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class OutputWriters {

    private OutputWriters() {
    }

    public static Map<String, OutputWriter> defaultWriters() {
        List<OutputWriter> writers = List.of(
                new XmlOutputWriter(),
                new JsonOutputWriter(),
                new JsonLinesOutputWriter(),
                new CsvOutputWriter(),
                new MarkdownOutputWriter());
        Map<String, OutputWriter> byFormat = new LinkedHashMap<>();
        for (OutputWriter writer : writers) {
            byFormat.put(writer.formatName(), writer);
        }
        return Map.copyOf(byFormat);
    }
}
