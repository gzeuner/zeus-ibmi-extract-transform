package de.zeus.ibmi.infrastructure.output;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.zeus.ibmi.transform.QueryResult;
import de.zeus.ibmi.transform.QueryResultFixture;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Objects;
import org.junit.jupiter.api.Test;

class OutputWritersGoldenFileTest {

  private final QueryResult sampleResult = QueryResultFixture.sampleResult();

  @Test
  void xmlWriter_shouldMatchGoldenFile() throws Exception {
    assertGolden(new XmlOutputWriter(), "/golden/output/sample.xml");
  }

  @Test
  void jsonWriter_shouldMatchGoldenFile() throws Exception {
    assertGolden(new JsonOutputWriter(), "/golden/output/sample.json");
  }

  @Test
  void jsonlWriter_shouldMatchGoldenFile() throws Exception {
    assertGolden(new JsonlOutputWriter(), "/golden/output/sample.jsonl");
  }

  @Test
  void csvWriter_shouldMatchGoldenFile() throws Exception {
    assertGolden(new CsvOutputWriter(), "/golden/output/sample.csv");
  }

  @Test
  void markdownWriter_shouldMatchGoldenFile() throws Exception {
    assertGolden(new MarkdownOutputWriter(), "/golden/output/sample.md");
  }

  @Test
  void htmlWriter_shouldMatchGoldenFile() throws Exception {
    assertGolden(new HtmlOutputWriter(), "/golden/output/sample.html");
  }

  @Test
  void htmlWriter_darkThemeWithCustomCssAndWithoutManifest_shouldMatchGoldenFile()
      throws Exception {
    Path customCssPath =
        Path.of(
            Objects.requireNonNull(getClass().getResource("/golden/output/sample-html-custom.css"))
                .toURI());
    HtmlOutputWriter writer =
        new HtmlOutputWriter(
            new HtmlOutputWriter.HtmlRenderOptions("dark", customCssPath.toString(), false));
    assertGolden(writer, "/golden/output/sample-html-dark-no-manifest.html");
  }

  private void assertGolden(AbstractStringOutputWriter writer, String resourcePath)
      throws Exception {
    String actual = normalize(writer.render(sampleResult));
    String expected = normalize(resourceAsString(resourcePath));
    assertEquals(expected, actual);
  }

  private static String resourceAsString(String resourcePath) throws IOException {
    try (InputStream in = OutputWritersGoldenFileTest.class.getResourceAsStream(resourcePath)) {
      if (in == null) {
        throw new IOException("Resource not found: " + resourcePath);
      }
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private static String normalize(String input) {
    return input.replace("\r\n", "\n");
  }
}
