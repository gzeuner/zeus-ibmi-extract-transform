package de.zeus.ibmi.selection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.zeus.ibmi.config.AppConfig;
import de.zeus.ibmi.config.OutputFormat;
import de.zeus.ibmi.connection.DriverManagerJdbcConnectionFactory;
import de.zeus.ibmi.output.OutputExportService;
import de.zeus.ibmi.output.OutputWriters;
import de.zeus.ibmi.runmanifest.RunManifest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RunSelectionAndExportUseCaseTest {

    private static final String DB_URL = "jdbc:h2:mem:zeusibmi_usecase;MODE=DB2;DB_CLOSE_DELAY=-1";

    @BeforeEach
    void setupSchema() throws Exception {
        try (Connection con = DriverManager.getConnection(DB_URL, "sa", "");
             Statement stmt = con.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS TEST_DATA");
            stmt.execute("CREATE TABLE TEST_DATA (ID INT PRIMARY KEY, NAME VARCHAR(200))");
            stmt.execute("INSERT INTO TEST_DATA (ID, NAME) VALUES (1, 'Alice')");
        }
    }

    @Test
    void run_shouldProduceSuccessManifestWithFiles() throws Exception {
        Path outputDir = Files.createTempDirectory("zeus-ibmi-usecase-");
        RunSelectionAndExportUseCase useCase = new RunSelectionAndExportUseCase(
                new ReadOnlyJdbcQueryExecutor(
                        new DriverManagerJdbcConnectionFactory(),
                        new ReadOnlyQueryGuard()),
                new OutputExportService(OutputWriters.defaultWriters()),
                "zeus-ibmi-extract-transform",
                "0.1.0-SNAPSHOT");

        AppConfig config = new AppConfig(
                "org.h2.Driver",
                DB_URL,
                "sa",
                "",
                null,
                "SELECT ID, NAME FROM TEST_DATA ORDER BY ID",
                outputDir.toString(),
                List.of(OutputFormat.XML, OutputFormat.JSON, OutputFormat.CSV, OutputFormat.MD),
                true,
                null,
                null,
                true);

        RunManifest manifest = useCase.run(
                config,
                "test-config.properties",
                "SELECT ID, NAME FROM TEST_DATA ORDER BY ID");

        assertEquals("SUCCESS", manifest.status());
        assertEquals("zeus-ibmi-extract-transform", manifest.toolName());
        assertEquals("0.1.0-SNAPSHOT", manifest.toolVersion());
        assertTrue(manifest.durationMillis() >= 0L);
        assertTrue(manifest.queryHash().startsWith("sha256:"));
        assertTrue(manifest.queryPreview().contains("SELECT ID, NAME"));
        assertEquals("test-config.properties", manifest.configSource());
        assertEquals(4, manifest.outputFiles().size());
        assertEquals(4, manifest.outputFormats().size());
        assertEquals(1, manifest.rowCount());
        assertEquals(2, manifest.columnCount());
        assertTrue(manifest.errorClass().isEmpty());
    }

    @Test
    void run_shouldProduceFailedManifestWithErrorClass() throws Exception {
        Path outputDir = Files.createTempDirectory("zeus-ibmi-usecase-fail-");
        RunSelectionAndExportUseCase useCase = new RunSelectionAndExportUseCase(
                new ReadOnlyJdbcQueryExecutor(
                        new DriverManagerJdbcConnectionFactory(),
                        new ReadOnlyQueryGuard()),
                new OutputExportService(OutputWriters.defaultWriters()),
                "zeus-ibmi-extract-transform",
                "0.1.0-SNAPSHOT");

        AppConfig config = new AppConfig(
                "org.h2.Driver",
                DB_URL,
                "sa",
                "",
                null,
                "SELECT ID, NAME FROM MISSING_TABLE",
                outputDir.toString(),
                List.of(OutputFormat.XML),
                true,
                null,
                null,
                true);

        RunManifest manifest = useCase.run(
                config,
                "test-config.properties",
                "SELECT ID, NAME FROM MISSING_TABLE");

        assertEquals("FAILED", manifest.status());
        assertTrue(manifest.errorClass().contains("QueryExecutionException"));
        assertFalse(manifest.errorMessage().isEmpty());
        assertTrue(manifest.outputFiles().isEmpty());
        assertTrue(manifest.queryHash().startsWith("sha256:"));
        assertTrue(manifest.queryPreview().contains("MISSING_TABLE"));
    }
}
