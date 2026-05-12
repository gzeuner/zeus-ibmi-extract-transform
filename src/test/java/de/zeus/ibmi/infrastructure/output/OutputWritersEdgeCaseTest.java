package de.zeus.ibmi.infrastructure.output;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.zeus.ibmi.transform.ColumnDefinition;
import de.zeus.ibmi.transform.QueryResult;
import de.zeus.ibmi.transform.RecordRow;
import java.sql.Types;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OutputWritersEdgeCaseTest {

  @Test
  void writers_shouldHandleNullsAndSpecialCharacters() {
    QueryResult result = edgeCaseResult();

    String xml = new XmlOutputWriter().render(result);
    String json = new JsonOutputWriter().render(result);
    String jsonl = new JsonlOutputWriter().render(result);
    String csv = new CsvOutputWriter().render(result);
    String md = new MarkdownOutputWriter().render(result);
    String html = new HtmlOutputWriter().render(result);

    assertTrue(xml.contains("Müller &amp; Söhne"));
    assertTrue(xml.contains("&quot;quoted&quot;"));
    assertTrue(xml.contains("Line1\nLine2"));
    assertTrue(xml.contains("path=\"A|B\""));
    assertTrue(xml.contains("<value></value>"));

    assertTrue(json.contains("Müller & Söhne"));
    assertTrue(json.contains("\\\"quoted\\\""));
    assertTrue(json.contains("Line1\\nLine2"));
    assertTrue(json.contains("\"A|B\""));
    assertTrue(json.contains("\"value\": \"\""));

    assertTrue(jsonl.contains("\"A|B\":\"Müller & Söhne\""));
    assertTrue(jsonl.contains("\"NOTES\":\"\\\"quoted\\\";x\""));
    assertTrue(jsonl.contains("\"A|B\":\"Line1\\nLine2\""));
    assertTrue(jsonl.contains("\"NOTES\":null"));
    assertTrue(jsonl.contains("\"A|B\":\"A,B\""));

    assertTrue(csv.contains("A|B;NOTES"));
    assertTrue(csv.contains("Müller & Söhne"));
    assertTrue(csv.contains("\"\"\"quoted\"\";x\""));
    assertTrue(csv.contains("\"Line1\nLine2\""));
    assertTrue(csv.contains("\"Line1\nLine2\";"));
    assertTrue(csv.contains("\"A,B\";comma"));

    assertTrue(md.contains("A\\|B"));
    assertTrue(md.contains("\"quoted\";x"));
    assertTrue(md.contains("Line1<br/>Line2"));

    assertTrue(html.contains("title=\"JDBC Type: VARCHAR\""));
    assertTrue(html.contains("Müller &amp; Söhne"));
    assertTrue(html.contains("&quot;quoted&quot;;x"));
    assertTrue(html.contains("Line1\nLine2"));
    assertTrue(html.contains("<th scope=\"col\""));
    assertTrue(html.contains("id=\"zeus-html-manifest\""));
  }

  @Test
  void writers_shouldHandleEmptyResultSet() {
    QueryResult empty =
        new QueryResult(
            "SELECT X", List.of(new ColumnDefinition("C1", Types.VARCHAR, "VARCHAR")), List.of());

    assertEquals(
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<export>\n</export>\n",
        new XmlOutputWriter().render(empty));
    assertEquals("[\n]\n", new JsonOutputWriter().render(empty));
    assertEquals("", new JsonlOutputWriter().render(empty));
    assertEquals("C1\nVARCHAR\n", new CsvOutputWriter().render(empty));
    assertEquals("| C1 |\n| --- |\n", new MarkdownOutputWriter().render(empty));
    assertTrue(new HtmlOutputWriter().render(empty).contains("<table class=\"export-table\">"));
  }

  private static QueryResult edgeCaseResult() {
    List<ColumnDefinition> columns =
        List.of(
            new ColumnDefinition("A|B", Types.VARCHAR, "VARCHAR"),
            new ColumnDefinition("NOTES", Types.VARCHAR, "VARCHAR"));

    Map<String, Object> row1 = new LinkedHashMap<>();
    row1.put("A|B", "Müller & Söhne");
    row1.put("NOTES", "\"quoted\";x");

    Map<String, Object> row2 = new LinkedHashMap<>();
    row2.put("A|B", "Line1\nLine2");
    row2.put("NOTES", null);

    Map<String, Object> row3 = new LinkedHashMap<>();
    row3.put("A|B", "A,B");
    row3.put("NOTES", "comma");

    return new QueryResult(
        "SELECT \"A|B\", NOTES FROM T",
        columns,
        List.of(new RecordRow(row1), new RecordRow(row2), new RecordRow(row3)));
  }
}
