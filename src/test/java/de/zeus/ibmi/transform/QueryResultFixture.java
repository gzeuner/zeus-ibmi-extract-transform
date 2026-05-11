package de.zeus.ibmi.transform;

import java.sql.Types;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class QueryResultFixture {

    private QueryResultFixture() {
    }

    public static QueryResult sampleResult() {
        List<ColumnDefinition> columns = List.of(
                new ColumnDefinition("ID", Types.INTEGER, "INTEGER"),
                new ColumnDefinition("NAME", Types.VARCHAR, "VARCHAR"));

        Map<String, Object> row1 = new LinkedHashMap<>();
        row1.put("ID", 1);
        row1.put("NAME", "Alice");

        Map<String, Object> row2 = new LinkedHashMap<>();
        row2.put("ID", 2);
        row2.put("NAME", "Bob|Builder;\"X\"");

        return new QueryResult(
                "SELECT ID, NAME FROM TEST_DATA ORDER BY ID",
                columns,
                List.of(new RecordRow(row1), new RecordRow(row2)));
    }
}
