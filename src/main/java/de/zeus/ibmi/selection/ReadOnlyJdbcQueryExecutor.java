package de.zeus.ibmi.selection;

import de.zeus.ibmi.connection.JdbcConnectionFactory;
import de.zeus.ibmi.infrastructure.config.AppConfig;
import de.zeus.ibmi.transform.ColumnDefinition;
import de.zeus.ibmi.transform.QueryResult;
import de.zeus.ibmi.transform.RecordRow;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ReadOnlyJdbcQueryExecutor {

  private final JdbcConnectionFactory connectionFactory;
  private final ReadOnlyQueryGuard queryGuard;

  public ReadOnlyJdbcQueryExecutor(
      JdbcConnectionFactory connectionFactory, ReadOnlyQueryGuard queryGuard) {
    this.connectionFactory = connectionFactory;
    this.queryGuard = queryGuard;
  }

  public QueryResult execute(AppConfig config) {
    String normalizedQuery = queryGuard.validateOrNormalize(config.query());
    try (Connection connection =
            connectionFactory.open(
                config.databaseDriver(),
                config.databaseUrl(),
                config.username(),
                config.password());
        PreparedStatement stmt =
            connection.prepareStatement(
                normalizedQuery, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {

      if (config.fetchSize() != null) {
        stmt.setFetchSize(config.fetchSize());
      }
      if (config.queryTimeoutSeconds() != null) {
        stmt.setQueryTimeout(config.queryTimeoutSeconds());
      }

      try (ResultSet rs = stmt.executeQuery()) {
        ResultSetMetaData meta = rs.getMetaData();
        List<ColumnDefinition> columns = extractColumns(meta);
        List<RecordRow> rows = extractRows(rs, columns);
        return new QueryResult(normalizedQuery, columns, rows);
      }
    } catch (SQLException ex) {
      throw new QueryExecutionException("Read-only query execution failed.", ex);
    }
  }

  private static List<ColumnDefinition> extractColumns(ResultSetMetaData meta) throws SQLException {
    int columnCount = meta.getColumnCount();
    List<ColumnDefinition> columns = new ArrayList<>(columnCount);
    for (int i = 1; i <= columnCount; i++) {
      String label = meta.getColumnLabel(i);
      String name = (label == null || label.isBlank()) ? meta.getColumnName(i) : label;
      columns.add(new ColumnDefinition(name, meta.getColumnType(i), meta.getColumnTypeName(i)));
    }
    return columns;
  }

  private static List<RecordRow> extractRows(ResultSet rs, List<ColumnDefinition> columns)
      throws SQLException {
    List<RecordRow> rows = new ArrayList<>();
    while (rs.next()) {
      Map<String, Object> values = new LinkedHashMap<>();
      for (int i = 0; i < columns.size(); i++) {
        ColumnDefinition column = columns.get(i);
        Object value = rs.getObject(i + 1);
        values.put(column.name(), value);
      }
      rows.add(new RecordRow(values));
    }
    return rows;
  }
}
