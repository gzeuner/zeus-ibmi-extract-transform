package de.zeus.ibmi.infrastructure.output;

import de.zeus.ibmi.transform.ColumnDefinition;
import de.zeus.ibmi.transform.QueryResult;
import de.zeus.ibmi.transform.RecordRow;

public final class MarkdownOutputWriter extends AbstractStringOutputWriter {

  @Override
  public String formatName() {
    return "md";
  }

  @Override
  public String render(QueryResult result) {
    StringBuilder out = new StringBuilder();

    out.append("|");
    for (ColumnDefinition column : result.columns()) {
      out.append(" ").append(escapeMd(column.name())).append(" |");
    }
    out.append("\n");

    out.append("|");
    for (int i = 0; i < result.columns().size(); i++) {
      out.append(" --- |");
    }
    out.append("\n");

    for (RecordRow row : result.rows()) {
      out.append("|");
      for (ColumnDefinition column : result.columns()) {
        Object value = row.value(column.name());
        out.append(" ").append(escapeMd(value == null ? "" : String.valueOf(value))).append(" |");
      }
      out.append("\n");
    }

    return out.toString();
  }

  private static String escapeMd(String value) {
    if (value == null) {
      return "";
    }
    return value
        .replace("|", "\\|")
        .replace("\r\n", "<br/>")
        .replace("\n", "<br/>")
        .replace("\r", "<br/>");
  }
}
