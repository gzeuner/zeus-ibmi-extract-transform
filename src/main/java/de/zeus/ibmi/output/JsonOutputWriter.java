package de.zeus.ibmi.output;

import de.zeus.ibmi.transform.ColumnDefinition;
import de.zeus.ibmi.transform.QueryResult;
import de.zeus.ibmi.transform.RecordRow;

public final class JsonOutputWriter implements OutputWriter {

    @Override
    public String formatName() {
        return "json";
    }

    @Override
    public String render(QueryResult result) {
        StringBuilder out = new StringBuilder();
        out.append("[\n");
        for (int rowIndex = 0; rowIndex < result.rows().size(); rowIndex++) {
            RecordRow row = result.rows().get(rowIndex);
            out.append("  {");
            for (int i = 0; i < result.columns().size(); i++) {
                ColumnDefinition column = result.columns().get(i);
                Object value = row.value(column.name());
                if (i > 0) {
                    out.append(",");
                }
                out.append("\n    \"")
                        .append(escapeJson(column.name()))
                        .append("\": {\"value\": \"")
                        .append(escapeJson(value == null ? "" : String.valueOf(value)))
                        .append("\", \"type\": \"")
                        .append(escapeJson(column.jdbcTypeName()))
                        .append("\"}");
            }
            out.append("\n  }");
            if (rowIndex < result.rows().size() - 1) {
                out.append(",");
            }
            out.append("\n");
        }
        out.append("]\n");
        return out.toString();
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
