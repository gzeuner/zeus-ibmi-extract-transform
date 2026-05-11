package de.zeus.ibmi.transform;

import java.util.List;

public record QueryResult(
        String normalizedQuery,
        List<ColumnDefinition> columns,
        List<RecordRow> rows) {

    public QueryResult {
        columns = columns == null ? List.of() : List.copyOf(columns);
        rows = rows == null ? List.of() : List.copyOf(rows);
    }

    public int rowCount() {
        return rows.size();
    }
}
