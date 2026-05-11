package de.zeus.ibmi.selection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.zeus.ibmi.config.AppConfig;
import de.zeus.ibmi.config.OutputFormat;
import de.zeus.ibmi.connection.DriverManagerJdbcConnectionFactory;
import de.zeus.ibmi.transform.QueryResult;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReadOnlyJdbcQueryExecutorTest {

    private static final String DB_URL = "jdbc:h2:mem:zeusibmi;MODE=DB2;DB_CLOSE_DELAY=-1";

    @BeforeEach
    void setupSchema() throws Exception {
        try (Connection con = DriverManager.getConnection(DB_URL, "sa", "");
             Statement stmt = con.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS TEST_DATA");
            stmt.execute("CREATE TABLE TEST_DATA (ID INT PRIMARY KEY, NAME VARCHAR(200))");
            stmt.execute("INSERT INTO TEST_DATA (ID, NAME) VALUES (1, 'Alice')");
            stmt.execute("INSERT INTO TEST_DATA (ID, NAME) VALUES (2, 'Bob')");
        }
    }

    @Test
    void execute_shouldReturnRowsForReadOnlySelect() throws Exception {
        ReadOnlyJdbcQueryExecutor executor = new ReadOnlyJdbcQueryExecutor(
                new DriverManagerJdbcConnectionFactory(),
                new ReadOnlyQueryGuard());

        AppConfig config = new AppConfig(
                "org.h2.Driver",
                DB_URL,
                "sa",
                "",
                null,
                "SELECT ID, NAME FROM TEST_DATA ORDER BY ID",
                null,
                Files.createTempDirectory("out").toString(),
                List.of(OutputFormat.JSON),
                true,
                null,
                null,
                true);

        QueryResult result = executor.execute(config);

        assertEquals(2, result.rows().size());
        assertEquals("SELECT ID, NAME FROM TEST_DATA ORDER BY ID", result.normalizedQuery());
        assertEquals("ID", result.columns().get(0).name());
        assertEquals("NAME", result.columns().get(1).name());
        assertEquals(1, result.rows().get(0).value("ID"));
        assertEquals("Alice", result.rows().get(0).value("NAME"));
    }

    @Test
    void execute_shouldRejectNonReadOnlyQuery() throws Exception {
        ReadOnlyJdbcQueryExecutor executor = new ReadOnlyJdbcQueryExecutor(
                new DriverManagerJdbcConnectionFactory(),
                new ReadOnlyQueryGuard());

        AppConfig config = new AppConfig(
                "org.h2.Driver",
                DB_URL,
                "sa",
                "",
                null,
                "UPDATE TEST_DATA SET NAME='X' WHERE ID=1",
                null,
                Files.createTempDirectory("out").toString(),
                List.of(OutputFormat.JSON),
                true,
                null,
                null,
                true);

        assertThrows(QueryGuardException.class, () -> executor.execute(config));
    }

    @Test
    void execute_shouldWrapSqlErrors() throws Exception {
        ReadOnlyJdbcQueryExecutor executor = new ReadOnlyJdbcQueryExecutor(
                new DriverManagerJdbcConnectionFactory(),
                new ReadOnlyQueryGuard());

        AppConfig config = new AppConfig(
                "org.h2.Driver",
                DB_URL,
                "sa",
                "",
                null,
                "SELECT DOES_NOT_EXIST FROM TEST_DATA",
                null,
                Files.createTempDirectory("out").toString(),
                List.of(OutputFormat.JSON),
                true,
                null,
                null,
                true);

        QueryExecutionException ex = assertThrows(QueryExecutionException.class, () -> executor.execute(config));
        assertTrue(ex.getMessage().contains("Read-only query execution failed"));
    }
}
