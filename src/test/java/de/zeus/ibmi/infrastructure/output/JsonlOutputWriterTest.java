package de.zeus.ibmi.infrastructure.output;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.zeus.ibmi.transform.ColumnDefinition;
import de.zeus.ibmi.transform.QueryResult;
import de.zeus.ibmi.transform.RecordRow;
import java.math.BigDecimal;
import java.sql.Types;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JsonlOutputWriterTest {

  @Test
  void render_shouldWriteJsonObjectPerLineWithTypedValues() {
    List<ColumnDefinition> columns =
        List.of(
            new ColumnDefinition("ID", Types.INTEGER, "INTEGER"),
            new ColumnDefinition("ACTIVE", Types.BOOLEAN, "BOOLEAN"),
            new ColumnDefinition("RATIO", Types.DECIMAL, "DECIMAL"),
            new ColumnDefinition("NOTES", Types.VARCHAR, "VARCHAR"),
            new ColumnDefinition("NULL_COL", Types.VARCHAR, "VARCHAR"));

    Map<String, Object> row = new LinkedHashMap<>();
    row.put("ID", 7);
    row.put("ACTIVE", true);
    row.put("RATIO", new BigDecimal("12.50"));
    row.put("NOTES", "Line1\nLine2 \"Q\"");
    row.put("NULL_COL", null);

    QueryResult result = new QueryResult("SELECT 1", columns, List.of(new RecordRow(row)));

    String jsonl = new JsonlOutputWriter().render(result);
    assertEquals(
        "{\"ID\":7,\"ACTIVE\":true,\"RATIO\":12.50,\"NOTES\":\"Line1\\nLine2 \\\"Q\\\"\",\"NULL_COL\":null}\n",
        jsonl);
  }
}
