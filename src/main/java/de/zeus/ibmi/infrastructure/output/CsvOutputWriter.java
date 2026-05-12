package de.zeus.ibmi.infrastructure.output;

import de.zeus.ibmi.transform.ColumnDefinition;
import de.zeus.ibmi.transform.QueryResult;
import de.zeus.ibmi.transform.RecordRow;
import java.util.stream.Collectors;

public final class CsvOutputWriter extends AbstractStringOutputWriter {

  @Override
  public String formatName() {
    return "csv";
  }

  @Override
  public String render(QueryResult result) {
    StringBuilder out = new StringBuilder();
    out.append(
        result.columns().stream()
            .map(ColumnDefinition::name)
            .map(CsvOutputWriter::escapeCsv)
            .collect(Collectors.joining(";")));
    out.append("\n");

    out.append(
        result.columns().stream()
            .map(ColumnDefinition::jdbcTypeName)
            .map(CsvOutputWriter::escapeCsv)
            .collect(Collectors.joining(";")));
    out.append("\n");

    for (RecordRow row : result.rows()) {
      out.append(
          result.columns().stream()
              .map(column -> row.value(column.name()))
              .map(value -> value == null ? "" : String.valueOf(value))
              .map(CsvOutputWriter::escapeCsv)
              .collect(Collectors.joining(";")));
      out.append("\n");
    }

    return out.toString();
  }

  private static String escapeCsv(String value) {
    if (value == null) {
      return "";
    }
    boolean needsQuoting =
        value.contains(";")
            || value.contains(",")
            || value.contains("\"")
            || value.contains("\n")
            || value.contains("\r");
    if (!needsQuoting) {
      return value;
    }
    return "\"" + value.replace("\"", "\"\"") + "\"";
  }
}
