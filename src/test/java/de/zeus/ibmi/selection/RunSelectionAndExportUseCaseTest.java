package de.zeus.ibmi.selection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.zeus.ibmi.connection.DriverManagerJdbcConnectionFactory;
import de.zeus.ibmi.infrastructure.config.AppConfig;
import de.zeus.ibmi.infrastructure.config.OutputFormat;
import de.zeus.ibmi.infrastructure.output.AbstractStringOutputWriter;
import de.zeus.ibmi.infrastructure.output.OutputExportService;
import de.zeus.ibmi.infrastructure.output.OutputWriter;
import de.zeus.ibmi.infrastructure.output.OutputWriters;
import de.zeus.ibmi.runmanifest.RunManifest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    RunSelectionAndExportUseCase useCase =
        new RunSelectionAndExportUseCase(
            new ReadOnlyJdbcQueryExecutor(
                new DriverManagerJdbcConnectionFactory(), new ReadOnlyQueryGuard()),
            new OutputExportService(OutputWriters.defaultWriters()),
            "zeus-ibmi-extract-transform",
            "0.1.0-SNAPSHOT");

    AppConfig config =
        new AppConfig(
            "org.h2.Driver",
            DB_URL,
            "sa",
            "",
            null,
            "SELECT ID, NAME FROM TEST_DATA ORDER BY ID",
            null,
            outputDir.toString(),
            List.of(
                OutputFormat.XML,
                OutputFormat.JSON,
                OutputFormat.JSONL,
                OutputFormat.CSV,
                OutputFormat.MD),
            true,
            null,
            null,
            true);

    RunManifest manifest =
        useCase.run(
            config,
            "test-config.properties",
            "CONFIG_INLINE",
            "query.sql",
            "SELECT ID, NAME FROM TEST_DATA ORDER BY ID");

    assertEquals("SUCCESS", manifest.status());
    assertEquals("zeus-ibmi-extract-transform", manifest.toolName());
    assertEquals("0.1.0-SNAPSHOT", manifest.toolVersion());
    assertTrue(manifest.durationMillis() >= 0L);
    assertTrue(manifest.queryHash().startsWith("sha256:"));
    assertTrue(manifest.queryPreview().contains("SELECT ID, NAME"));
    assertEquals("CONFIG_INLINE", manifest.querySourceType());
    assertEquals("query.sql", manifest.querySource());
    assertEquals("test-config.properties", manifest.configSource());
    assertEquals("<output-directory>", manifest.outputDirectory());
    assertEquals(5, manifest.outputFiles().size());
    assertEquals(5, manifest.outputFormats().size());
    assertEquals(1, manifest.rowCount());
    assertEquals(2, manifest.columnCount());
    assertTrue(manifest.errorClass().isEmpty());
    assertTrue(manifest.outputFiles().stream().allMatch(file -> file.sizeBytes() > 0));
    assertTrue(
        manifest.outputFiles().stream().allMatch(file -> file.sha256().startsWith("sha256:")));
    assertTrue(
        manifest.outputFiles().stream()
            .allMatch(file -> !file.path().contains(outputDir.toAbsolutePath().toString())));
    assertTrue(manifest.outputFiles().stream().anyMatch(file -> "jsonl".equals(file.format())));
  }

  @Test
  void run_shouldProduceFailedManifestWithErrorClass() throws Exception {
    Path outputDir = Files.createTempDirectory("zeus-ibmi-usecase-fail-");
    RunSelectionAndExportUseCase useCase =
        new RunSelectionAndExportUseCase(
            new ReadOnlyJdbcQueryExecutor(
                new DriverManagerJdbcConnectionFactory(), new ReadOnlyQueryGuard()),
            new OutputExportService(OutputWriters.defaultWriters()),
            "zeus-ibmi-extract-transform",
            "0.1.0-SNAPSHOT");

    AppConfig config =
        new AppConfig(
            "org.h2.Driver",
            DB_URL,
            "sa",
            "",
            null,
            "SELECT ID, NAME FROM MISSING_TABLE",
            null,
            outputDir.toString(),
            List.of(OutputFormat.XML),
            true,
            null,
            null,
            true);

    RunManifest manifest =
        useCase.run(
            config,
            "test-config.properties",
            "CONFIG_INLINE",
            "query.sql",
            "SELECT ID, NAME FROM MISSING_TABLE");

    assertEquals("FAILED", manifest.status());
    assertEquals("<output-directory>", manifest.outputDirectory());
    assertTrue(manifest.errorClass().contains("QueryExecutionException"));
    assertFalse(manifest.errorMessage().isEmpty());
    assertTrue(manifest.outputFiles().isEmpty());
    assertTrue(manifest.queryHash().startsWith("sha256:"));
    assertTrue(manifest.queryPreview().contains("MISSING_TABLE"));
    assertEquals("CONFIG_INLINE", manifest.querySourceType());
    assertEquals("query.sql", manifest.querySource());
  }

  @Test
  void run_shouldCaptureAlreadyWrittenOutputFilesWhenLaterFormatFails() throws Exception {
    Path outputDir = Files.createTempDirectory("zeus-ibmi-usecase-partial-fail-");
    Map<String, OutputWriter> writers = new LinkedHashMap<>();
    writers.put("xml", OutputWriters.defaultWriters().get("xml"));
    writers.put("json", failingWriter("json"));
    RunSelectionAndExportUseCase useCase =
        new RunSelectionAndExportUseCase(
            new ReadOnlyJdbcQueryExecutor(
                new DriverManagerJdbcConnectionFactory(), new ReadOnlyQueryGuard()),
            new OutputExportService(Map.copyOf(writers)),
            "zeus-ibmi-extract-transform",
            "0.1.0-SNAPSHOT");

    AppConfig config =
        new AppConfig(
            "org.h2.Driver",
            DB_URL,
            "sa",
            "",
            null,
            "SELECT ID, NAME FROM TEST_DATA ORDER BY ID",
            null,
            outputDir.toString(),
            List.of(OutputFormat.XML, OutputFormat.JSON),
            true,
            null,
            null,
            true);

    RunManifest manifest =
        useCase.run(
            config,
            "test-config.properties",
            "CONFIG_INLINE",
            "query.sql",
            "SELECT ID, NAME FROM TEST_DATA ORDER BY ID");

    assertEquals("FAILED", manifest.status());
    assertEquals("<output-directory>", manifest.outputDirectory());
    assertEquals("CONFIG_INLINE", manifest.querySourceType());
    assertEquals("query.sql", manifest.querySource());
    assertTrue(manifest.errorClass().contains("OutputWriteException"));
    assertEquals(1, manifest.outputFiles().size());
    assertEquals("xml", manifest.outputFiles().get(0).format());
    assertTrue(manifest.outputFiles().get(0).sha256().startsWith("sha256:"));
    assertTrue(manifest.outputFiles().get(0).sizeBytes() > 0);
  }

  private static OutputWriter failingWriter(String formatName) {
    return new AbstractStringOutputWriter() {
      @Override
      public String formatName() {
        return formatName;
      }

      @Override
      public String render(de.zeus.ibmi.transform.QueryResult result) {
        throw new IllegalStateException("forced render failure");
      }
    };
  }
}
