package de.zeus.ibmi.domain;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public sealed interface OutputFormat
    permits OutputFormat.Xml,
        OutputFormat.Json,
        OutputFormat.Jsonl,
        OutputFormat.Csv,
        OutputFormat.Markdown {

  OutputFormat XML = new Xml();
  OutputFormat JSON = new Json();
  OutputFormat JSONL = new Jsonl();
  OutputFormat CSV = new Csv();
  OutputFormat MD = new Markdown();

  String id();

  static OutputFormat fromId(String formatId) {
    if (formatId == null || formatId.isBlank()) {
      throw new IllegalArgumentException("Output format id is required");
    }
    String normalized = formatId.trim().toLowerCase(Locale.ROOT);
    OutputFormat format = byId().get(normalized);
    if (format == null) {
      throw new IllegalArgumentException("Unsupported output format: " + normalized);
    }
    return format;
  }

  private static Map<String, OutputFormat> byId() {
    Map<String, OutputFormat> formats = new LinkedHashMap<>();
    formats.put(XML.id(), XML);
    formats.put(JSON.id(), JSON);
    formats.put(JSONL.id(), JSONL);
    formats.put(CSV.id(), CSV);
    formats.put(MD.id(), MD);
    return Map.copyOf(formats);
  }

  record Xml() implements OutputFormat {
    @Override
    public String id() {
      return "xml";
    }
  }

  record Json() implements OutputFormat {
    @Override
    public String id() {
      return "json";
    }
  }

  record Jsonl() implements OutputFormat {
    @Override
    public String id() {
      return "jsonl";
    }
  }

  record Csv() implements OutputFormat {
    @Override
    public String id() {
      return "csv";
    }
  }

  record Markdown() implements OutputFormat {
    @Override
    public String id() {
      return "md";
    }
  }
}
