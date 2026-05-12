package de.zeus.ibmi.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.zeus.ibmi.infrastructure.config.AppConfig;
import de.zeus.ibmi.infrastructure.config.ConfigValidationException;
import de.zeus.ibmi.infrastructure.config.OutputFormat;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class QuerySourceResolverTest {

  @Test
  void resolve_shouldPreferCliInlineOverAllOthers() throws Exception {
    Path queryFile = Files.createTempFile("query-source-cli-inline-", ".sql");
    Files.writeString(queryFile, "SELECT 2", StandardCharsets.UTF_8);

    QuerySource source =
        new QuerySourceResolver()
            .resolve(
                config("SELECT 1", queryFile.toString()),
                Map.of("query.sql", "SELECT 99", "query.file", queryFile.toString()),
                Path.of("config/example.application.properties"));

    assertEquals(QuerySourceType.CLI_INLINE, source.sourceType());
    assertEquals("CLI inline", source.source());
    assertEquals("SELECT 99", source.queryText());
    assertTrue(source.multipleSourcesConfigured());
  }

  @Test
  void resolve_shouldPreferCliFileOverConfigInlineAndConfigFile() throws Exception {
    Path queryFile = Files.createTempFile("query-source-cli-file-", ".sql");
    Files.writeString(queryFile, "SELECT 123", StandardCharsets.UTF_8);

    QuerySource source =
        new QuerySourceResolver()
            .resolve(
                config("SELECT 1", "queries/from-config.sql"),
                Map.of("query.file", queryFile.toString()),
                Path.of("config/example.application.properties"));

    assertEquals(QuerySourceType.CLI_FILE, source.sourceType());
    assertEquals("SELECT 123", source.queryText());
    assertEquals(queryFile.getFileName().toString(), source.source());
  }

  @Test
  void resolve_shouldPreferConfigInlineOverConfigFile() throws Exception {
    Path queryFile = Files.createTempFile("query-source-config-inline-", ".sql");
    Files.writeString(queryFile, "SELECT 2", StandardCharsets.UTF_8);

    QuerySource source =
        new QuerySourceResolver()
            .resolve(
                config("SELECT 1", queryFile.toString()),
                Map.of(),
                Path.of("config/example.application.properties"));

    assertEquals(QuerySourceType.CONFIG_INLINE, source.sourceType());
    assertEquals("Config inline", source.source());
    assertEquals("SELECT 1", source.queryText());
  }

  @Test
  void resolve_shouldLoadQueryFromConfigFile() throws Exception {
    Path configDir = Files.createTempDirectory("query-source-config-file-");
    Path queryFile = configDir.resolve("customers.sql");
    Files.writeString(queryFile, "SELECT 1 AS ID", StandardCharsets.UTF_8);

    QuerySource source =
        new QuerySourceResolver()
            .resolve(config(null, "customers.sql"), Map.of(), configDir.resolve("app.properties"));

    assertEquals(QuerySourceType.CONFIG_FILE, source.sourceType());
    assertEquals("customers.sql", source.source());
    assertEquals("SELECT 1 AS ID", source.queryText());
  }

  @Test
  void resolve_shouldFailWhenQueryFileMissing() {
    Path missing = Path.of("does-not-exist", "missing.sql");
    ConfigValidationException ex =
        assertThrows(
            ConfigValidationException.class,
            () ->
                new QuerySourceResolver()
                    .resolve(
                        config(null, null),
                        Map.of("query.file", missing.toString()),
                        Path.of("config/example.application.properties")));

    assertTrue(ex.getMessage().contains("Query file does not exist"));
  }

  @Test
  void resolve_shouldFailWhenQueryFileEmpty() throws Exception {
    Path queryFile = Files.createTempFile("query-source-empty-", ".sql");
    Files.writeString(queryFile, "   \n\r\n  ", StandardCharsets.UTF_8);

    ConfigValidationException ex =
        assertThrows(
            ConfigValidationException.class,
            () ->
                new QuerySourceResolver()
                    .resolve(
                        config(null, queryFile.toString()),
                        Map.of(),
                        Path.of("config/example.application.properties")));

    assertTrue(ex.getMessage().contains("Query file is empty"));
  }

  @Test
  void resolve_shouldFailWhenQueryFilePointsToDirectory() throws Exception {
    Path directory = Files.createTempDirectory("query-source-dir-");

    ConfigValidationException ex =
        assertThrows(
            ConfigValidationException.class,
            () ->
                new QuerySourceResolver()
                    .resolve(
                        config(null, directory.toString()),
                        Map.of(),
                        Path.of("config/example.application.properties")));

    assertTrue(ex.getMessage().contains("points to a directory"));
  }

  @Test
  void resolve_shouldFailWhenFileContainsOnlyComments() throws Exception {
    Path queryFile = Files.createTempFile("query-source-comments-", ".sql");
    Files.writeString(queryFile, "-- comment only\n/* another */\n", StandardCharsets.UTF_8);

    ConfigValidationException ex =
        assertThrows(
            ConfigValidationException.class,
            () ->
                new QuerySourceResolver()
                    .resolve(
                        config(null, queryFile.toString()),
                        Map.of(),
                        Path.of("config/example.application.properties")));

    assertTrue(ex.getMessage().contains("only comments or whitespace"));
  }

  @Test
  void resolve_shouldFailWhenQueryFileNotReadable() throws Exception {
    Path queryFile = Files.createTempFile("query-source-not-readable-", ".sql");
    Files.writeString(queryFile, "SELECT 1", StandardCharsets.UTF_8);
    PosixFileAttributeView posixView =
        Files.getFileAttributeView(queryFile, PosixFileAttributeView.class);
    if (posixView == null) {
      return;
    }
    Set<PosixFilePermission> originalPermissions = posixView.readAttributes().permissions();
    try {
      Files.setPosixFilePermissions(queryFile, Set.of());
      ConfigValidationException ex =
          assertThrows(
              ConfigValidationException.class,
              () ->
                  new QuerySourceResolver()
                      .resolve(
                          config(null, queryFile.toString()),
                          Map.of(),
                          Path.of("config/example.application.properties")));

      assertTrue(ex.getMessage().contains("not readable"));
    } finally {
      Files.setPosixFilePermissions(queryFile, originalPermissions);
    }
  }

  @Test
  void resolve_shouldReadUtf8QueryFiles() throws Exception {
    Path queryFile = Files.createTempFile("query-source-utf8-", ".sql");
    Files.writeString(queryFile, "SELECT 'Müller' AS NAME", StandardCharsets.UTF_8);

    QuerySource source =
        new QuerySourceResolver()
            .resolve(
                config(null, queryFile.toString()),
                Map.of(),
                Path.of("config/example.application.properties"));

    assertEquals("SELECT 'Müller' AS NAME", source.queryText());
  }

  @Test
  void resolve_shouldNotLeakAbsolutePathInSourceMetadata() throws Exception {
    Path queryFile = Files.createTempFile("query-source-absolute-", ".sql");
    Files.writeString(queryFile, "SELECT 1", StandardCharsets.UTF_8);

    QuerySource source =
        new QuerySourceResolver()
            .resolve(
                config(null, queryFile.toAbsolutePath().toString()),
                Map.of(),
                Path.of("config/example.application.properties"));

    assertEquals(queryFile.getFileName().toString(), source.source());
    assertTrue(!source.source().contains("/home/"));
    assertTrue(!source.source().contains("C:\\\\Users"));
  }

  private static AppConfig config(String query, String queryFile) {
    return new AppConfig(
        "org.h2.Driver",
        "jdbc:h2:mem:test",
        "sa",
        "",
        null,
        query,
        queryFile,
        "./output",
        List.of(OutputFormat.JSON),
        true,
        null,
        null,
        true);
  }
}
