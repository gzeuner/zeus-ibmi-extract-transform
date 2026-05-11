package de.zeus.ibmi.output;

import de.zeus.ibmi.transform.ColumnDefinition;
import de.zeus.ibmi.transform.QueryResult;
import de.zeus.ibmi.transform.RecordRow;

public final class XmlOutputWriter implements OutputWriter {

    @Override
    public String formatName() {
        return "xml";
    }

    @Override
    public String render(QueryResult result) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<export>\n");
        for (RecordRow row : result.rows()) {
            xml.append("  <data>\n");
            for (ColumnDefinition column : result.columns()) {
                Object rawValue = row.value(column.name());
                String value = rawValue == null ? "" : String.valueOf(rawValue);
                xml.append("    <property path=\"")
                        .append(escapeXml(column.name()))
                        .append("\" type=\"")
                        .append(escapeXml(column.jdbcTypeName()))
                        .append("\">")
                        .append("<value>")
                        .append(escapeXml(value))
                        .append("</value>")
                        .append("</property>\n");
            }
            xml.append("  </data>\n");
        }
        xml.append("</export>\n");
        return xml.toString();
    }

    private static String escapeXml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
