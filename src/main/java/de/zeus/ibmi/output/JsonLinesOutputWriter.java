package de.zeus.ibmi.output;

import de.zeus.ibmi.transform.ColumnDefinition;
import de.zeus.ibmi.transform.QueryResult;
import de.zeus.ibmi.transform.RecordRow;
import java.math.BigDecimal;
import java.math.BigInteger;

public final class JsonLinesOutputWriter implements OutputWriter {

    @Override
    public String formatName() {
        return "jsonl";
    }

    @Override
    public String render(QueryResult result) {
        StringBuilder out = new StringBuilder();
        for (RecordRow row : result.rows()) {
            out.append("{");
            for (int i = 0; i < result.columns().size(); i++) {
                ColumnDefinition column = result.columns().get(i);
                if (i > 0) {
                    out.append(",");
                }
                out.append("\"")
                        .append(escapeJson(column.name()))
                        .append("\":");
                appendJsonValue(out, row.value(column.name()));
            }
            out.append("}\n");
        }
        return out.toString();
    }

    private static void appendJsonValue(StringBuilder out, Object value) {
        if (value == null) {
            out.append("null");
            return;
        }
        if (value instanceof Boolean bool) {
            out.append(bool);
            return;
        }
        if (value instanceof BigDecimal decimal) {
            out.append(decimal.toPlainString());
            return;
        }
        if (value instanceof BigInteger integer) {
            out.append(integer);
            return;
        }
        if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long) {
            out.append(value);
            return;
        }
        if (value instanceof Float floatValue) {
            if (Float.isFinite(floatValue)) {
                out.append(floatValue);
            } else {
                appendJsonString(out, floatValue.toString());
            }
            return;
        }
        if (value instanceof Double doubleValue) {
            if (Double.isFinite(doubleValue)) {
                out.append(doubleValue);
            } else {
                appendJsonString(out, doubleValue.toString());
            }
            return;
        }
        if (value instanceof Number number) {
            appendJsonString(out, number.toString());
            return;
        }
        appendJsonString(out, String.valueOf(value));
    }

    private static void appendJsonString(StringBuilder out, String value) {
        out.append("\"").append(escapeJson(value)).append("\"");
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder escaped = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\' -> escaped.append("\\\\");
                case '"' -> escaped.append("\\\"");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (c < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) c));
                    } else {
                        escaped.append(c);
                    }
                }
            }
        }
        return escaped.toString();
    }
}
