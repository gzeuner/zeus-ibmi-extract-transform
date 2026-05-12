package de.zeus.ibmi.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class OutputFormatParserTest {

  @Test
  void parseCsv_shouldAcceptJsonl() {
    List<OutputFormat> formats = OutputFormatParser.parseCsv("jsonl,xml,jsonl");
    assertEquals(List.of(OutputFormat.JSONL, OutputFormat.XML), formats);
  }

  @Test
  void parseCsv_shouldAcceptHtml() {
    List<OutputFormat> formats = OutputFormatParser.parseCsv("html,json");
    assertEquals(List.of(OutputFormat.HTML, OutputFormat.JSON), formats);
  }

  @Test
  void parseCsv_shouldKeepInvalidFormatsRejected() {
    assertThrows(ConfigValidationException.class, () -> OutputFormatParser.parseCsv("xml,banana"));
  }
}
