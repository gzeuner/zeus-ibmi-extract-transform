package de.zeus.ibmi.infrastructure.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.zeus.ibmi.common.version.VersionProvider;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class CliEndToEndTest {

  private static final Duration PROCESS_TIMEOUT = Duration.ofSeconds(30);

  @Test
  void help_shouldReturnExitCodeZeroAndUsage() throws Exception {
    ProcessResult result = runCli(List.of("--help"), Map.of());
    assertEquals(0, result.exitCode());
    assertTrue(result.stdout().contains("Usage:"));
  }

  @Test
  void version_shouldReturnExitCodeZeroAndVersion() throws Exception {
    ProcessResult result = runCli(List.of("--version"), Map.of());
    assertEquals(0, result.exitCode());
    assertTrue(result.stdout().contains(VersionProvider.DEVELOPMENT_FALLBACK_VERSION));
  }

  @Test
  void dryRun_withoutExecute_shouldNotWriteDataFilesAndShouldShowPlannedOutputs() throws Exception {
    Path outputDir = Files.createTempDirectory("cli-e2e-dryrun-out-");
    Path config =
        createConfigFile(
            outputDir,
            "org.h2.Driver",
            "jdbc:h2:mem:cli_e2e_dryrun;MODE=DB2;DB_CLOSE_DELAY=-1",
            "SELECT 1 AS X",
            "xml,jsonl",
            true,
            true,
            true);

    ProcessResult result =
        runCli(
            List.of("--config", config.toString()),
            Map.of("ZEUS_IBMI_DB_PASSWORD", "dummy-secret-dryrun"));

    assertEquals(0, result.exitCode());
    assertTrue(result.stdout().contains("Mode: DRY-RUN"));
    assertTrue(result.stdout().contains("Status: DRY_RUN"));
    assertTrue(result.stdout().contains("Planned output files"));
    assertTrue(result.stdout().contains("Hint: Add --execute to run read-only query execution."));
    assertFalse(result.stdout().contains("dummy-secret-dryrun"));
    assertFalse(result.stderr().contains("dummy-secret-dryrun"));

    List<Path> files = listFiles(outputDir);
    assertTrue(
        files.stream().anyMatch(path -> path.getFileName().toString().endsWith(".manifest.json")));
    assertFalse(
        files.stream()
            .anyMatch(path -> isDataOutputFile(path, "xml", "json", "jsonl", "csv", "md", "html")));
  }

  @Test
  void dryRun_withQueryFile_shouldUseConfigFileSourceAndWriteManifest() throws Exception {
    Path outputDir = Files.createTempDirectory("cli-e2e-dryrun-query-file-out-");
    Path queryFile = Files.createTempFile("cli-e2e-query-", ".sql");
    Files.writeString(queryFile, "SELECT 1 AS X", StandardCharsets.UTF_8);
    Path config =
        createConfigFileWithQueryFile(
            outputDir,
            "org.h2.Driver",
            "jdbc:h2:mem:cli_e2e_dryrun_query_file;MODE=DB2;DB_CLOSE_DELAY=-1",
            asPortablePath(queryFile),
            "json",
            true,
            true,
            true);

    ProcessResult result =
        runCli(
            List.of("--config", config.toString()),
            Map.of("ZEUS_IBMI_DB_PASSWORD", "dummy-secret-dryrun-qf"));

    assertEquals(0, result.exitCode());
    assertTrue(result.stdout().contains("Mode: DRY-RUN"));
    assertTrue(result.stdout().contains("Query Source: Config file: " + queryFile.getFileName()));
    assertFalse(result.stdout().contains("dummy-secret-dryrun-qf"));
    assertFalse(result.stderr().contains("dummy-secret-dryrun-qf"));

    Path manifest =
        listFiles(outputDir).stream()
            .filter(path -> path.getFileName().toString().endsWith(".manifest.json"))
            .max(Comparator.naturalOrder())
            .orElseThrow();
    String manifestJson = Files.readString(manifest, StandardCharsets.UTF_8);
    assertTrue(manifestJson.contains("\"querySourceType\":\"CONFIG_FILE\""));
    assertTrue(manifestJson.contains("\"querySource\":\"" + queryFile.getFileName() + "\""));
    assertTrue(manifestJson.contains("\"queryPreview\":\"SELECT 1 AS X\""));
  }

  @Test
  void execute_againstH2_shouldWriteOutputsAndSuccessManifestWithChecksums() throws Exception {
    Path outputDir = Files.createTempDirectory("cli-e2e-exec-out-");
    Path config =
        createConfigFile(
            outputDir,
            "org.h2.Driver",
            "jdbc:h2:mem:cli_e2e_execute;MODE=DB2;DB_CLOSE_DELAY=-1",
            "SELECT 1 AS ID, 'Müller' AS NAME, CAST(NULL AS VARCHAR) AS NOTES UNION ALL SELECT 2 AS ID, 'Bob|Builder;\"X\"' AS NAME, 'Line1' || CHAR(10) || 'Line2' AS NOTES ORDER BY ID",
            "xml,json,csv,md,jsonl,html",
            true,
            true,
            true);

    ProcessResult result =
        runCli(
            List.of("--config", config.toString(), "--execute"),
            Map.of("ZEUS_IBMI_DB_PASSWORD", "dummy-secret-exec"));

    assertEquals(0, result.exitCode());
    assertTrue(result.stdout().contains("Mode: EXECUTE"));
    assertTrue(result.stdout().contains("Status: SUCCESS"));
    assertTrue(result.stdout().contains("Row Count: 2"));
    assertTrue(result.stdout().contains("Output Files:"));
    assertFalse(result.stdout().contains("dummy-secret-exec"));
    assertFalse(result.stderr().contains("dummy-secret-exec"));

    List<Path> files = listFiles(outputDir);
    assertTrue(files.stream().anyMatch(path -> hasDataExtension(path, "xml")));
    assertTrue(files.stream().anyMatch(path -> hasDataExtension(path, "json")));
    assertTrue(files.stream().anyMatch(path -> hasDataExtension(path, "csv")));
    assertTrue(files.stream().anyMatch(path -> hasDataExtension(path, "md")));
    assertTrue(files.stream().anyMatch(path -> hasDataExtension(path, "jsonl")));
    assertTrue(files.stream().anyMatch(path -> hasDataExtension(path, "html")));

    Path manifest =
        files.stream()
            .filter(path -> path.getFileName().toString().endsWith(".manifest.json"))
            .max(Comparator.naturalOrder())
            .orElseThrow();
    String manifestJson = Files.readString(manifest, StandardCharsets.UTF_8);
    assertTrue(manifestJson.contains("\"outputDirectory\":\"<output-directory>\""));
    assertFalse(manifestJson.contains(outputDir.toString().replace('\\', '/')));
    assertTrue(manifestJson.contains("\"status\":\"SUCCESS\""));
    assertTrue(
        manifestJson.contains(
            "\"toolVersion\":\"" + VersionProvider.DEVELOPMENT_FALLBACK_VERSION + "\""));
    assertTrue(manifestJson.contains("\"rowCount\":2"));
    assertTrue(manifestJson.contains("\"format\":\"jsonl\""));
    assertTrue(manifestJson.contains("\"format\":\"html\""));
    assertTrue(manifestJson.contains("\"sizeBytes\":"));
    assertTrue(manifestJson.contains("\"sha256\":\"sha256:"));
  }

  @Test
  void execute_withQueryFile_againstH2_shouldWriteOutputsAndSanitizedQuerySource()
      throws Exception {
    Path outputDir = Files.createTempDirectory("cli-e2e-exec-query-file-out-");
    Path queryFile = Files.createTempFile("cli-e2e-exec-query-", ".sql");
    Files.writeString(queryFile, "SELECT 1 AS ID, 'demo' AS NAME", StandardCharsets.UTF_8);
    Path config =
        createConfigFileWithQueryFile(
            outputDir,
            "org.h2.Driver",
            "jdbc:h2:mem:cli_e2e_execute_query_file;MODE=DB2;DB_CLOSE_DELAY=-1",
            asPortablePath(queryFile),
            "json,jsonl",
            true,
            true,
            true);

    ProcessResult result =
        runCli(
            List.of("--config", config.toString(), "--execute"),
            Map.of("ZEUS_IBMI_DB_PASSWORD", "dummy-secret-exec-qf"));

    assertEquals(0, result.exitCode());
    assertTrue(result.stdout().contains("Status: SUCCESS"));
    assertTrue(result.stdout().contains("Query Source: Config file: " + queryFile.getFileName()));
    assertFalse(result.stdout().contains("dummy-secret-exec-qf"));
    assertFalse(result.stderr().contains("dummy-secret-exec-qf"));

    Path manifest =
        listFiles(outputDir).stream()
            .filter(path -> path.getFileName().toString().endsWith(".manifest.json"))
            .max(Comparator.naturalOrder())
            .orElseThrow();
    String manifestJson = Files.readString(manifest, StandardCharsets.UTF_8);
    assertTrue(manifestJson.contains("\"querySourceType\":\"CONFIG_FILE\""));
    assertTrue(manifestJson.contains("\"querySource\":\"" + queryFile.getFileName() + "\""));
    assertTrue(manifestJson.contains("\"queryHash\":\"sha256:"));
    assertTrue(manifestJson.contains("\"queryPreview\":\"SELECT 1 AS ID, 'demo' AS NAME\""));
    assertTrue(!manifestJson.contains(queryFile.toAbsolutePath().toString().replace('\\', '/')));
  }

  @Test
  void invalidQuery_shouldReturnGuardExitCodeAndNoExecution() throws Exception {
    Path outputDir = Files.createTempDirectory("cli-e2e-guard-out-");
    Path config =
        createConfigFile(
            outputDir,
            "org.h2.Driver",
            "jdbc:h2:mem:cli_e2e_guard;MODE=DB2;DB_CLOSE_DELAY=-1",
            "DELETE FROM TEST_DATA",
            "json",
            true,
            true,
            true);

    ProcessResult result = runCli(List.of("--config", config.toString(), "--execute"), Map.of());

    assertEquals(3, result.exitCode());
    assertTrue(result.stderr().contains("Error:"));
    assertTrue(result.stderr().contains("Only SELECT and WITH statements are allowed"));
    assertTrue(listFiles(outputDir).isEmpty());
  }

  @Test
  void jdbcConnectionError_shouldReturnJdbcExitCodeAndWriteFailureManifest() throws Exception {
    Path outputDir = Files.createTempDirectory("cli-e2e-jdbc-fail-out-");
    Path config =
        createConfigFile(
            outputDir,
            "example.DoesNotExistDriver",
            "jdbc:h2:mem:cli_e2e_jdbc_fail;MODE=DB2;DB_CLOSE_DELAY=-1",
            "SELECT 1 AS X",
            "json,jsonl",
            true,
            true,
            true);

    ProcessResult result =
        runCli(
            List.of("--config", config.toString(), "--execute"),
            Map.of("ZEUS_IBMI_DB_PASSWORD", "dummy-secret-jdbc"));

    assertEquals(4, result.exitCode());
    assertTrue(result.stdout().contains("Status: FAILED"));
    assertFalse(result.stdout().contains("dummy-secret-jdbc"));
    assertFalse(result.stderr().contains("dummy-secret-jdbc"));

    Path manifest =
        listFiles(outputDir).stream()
            .filter(path -> path.getFileName().toString().endsWith(".manifest.json"))
            .max(Comparator.naturalOrder())
            .orElseThrow();
    String manifestJson = Files.readString(manifest, StandardCharsets.UTF_8);
    assertTrue(manifestJson.contains("\"status\":\"FAILED\""));
    assertTrue(
        manifestJson.contains("\"errorClass\":\"de.zeus.ibmi.selection.QueryExecutionException\""));
  }

  @Test
  void execute_withoutConfig_shouldReturnConfigError() throws Exception {
    ProcessResult result = runCli(List.of("--execute"), Map.of());
    assertEquals(2, result.exitCode());
    assertTrue(result.stderr().contains("Missing required argument: --config <file>"));
  }

  @Test
  void unknownOption_shouldReturnConfigError() throws Exception {
    ProcessResult result = runCli(List.of("--unknown"), Map.of());
    assertEquals(2, result.exitCode());
    assertTrue(result.stderr().contains("Unknown argument: --unknown"));
  }

  @Test
  void missingOptionValue_shouldReturnConfigError() throws Exception {
    ProcessResult result = runCli(List.of("--config"), Map.of());
    assertEquals(2, result.exitCode());
    assertTrue(result.stderr().contains("Missing value for --config"));
  }

  @Test
  void optionInsteadOfValue_shouldReturnConfigError() throws Exception {
    ProcessResult result = runCli(List.of("--config", "--execute"), Map.of());
    assertEquals(2, result.exitCode());
    assertTrue(result.stderr().contains("Missing value for --config"));
  }

  @Test
  void missingQueryFile_shouldReturnConfigErrorExitCode2() throws Exception {
    Path outputDir = Files.createTempDirectory("cli-e2e-missing-query-file-out-");
    Path config =
        createConfigFileWithQueryFile(
            outputDir,
            "org.h2.Driver",
            "jdbc:h2:mem:cli_e2e_missing_query_file;MODE=DB2;DB_CLOSE_DELAY=-1",
            "missing-query-file.sql",
            "json",
            true,
            true,
            true);

    ProcessResult result =
        runCli(List.of("--config", config.toString()), Map.of("ZEUS_IBMI_DB_PASSWORD", "dummy"));
    assertEquals(2, result.exitCode());
    assertTrue(result.stderr().contains("Query file does not exist"));
  }

  private static ProcessResult runCli(List<String> args, Map<String, String> envOverrides)
      throws Exception {
    List<String> command = new ArrayList<>();
    command.add(javaCommand());
    command.add("-cp");
    command.add(System.getProperty("java.class.path"));
    command.add("de.zeus.ibmi.infrastructure.cli.Main");
    command.addAll(args);

    ProcessBuilder processBuilder = new ProcessBuilder(command);
    processBuilder.environment().putAll(envOverrides);

    Process process = processBuilder.start();
    CompletableFuture<String> stdoutFuture =
        CompletableFuture.supplyAsync(
            () -> {
              try {
                return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
              } catch (IOException ex) {
                throw new IllegalStateException(ex);
              }
            });
    CompletableFuture<String> stderrFuture =
        CompletableFuture.supplyAsync(
            () -> {
              try {
                return new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
              } catch (IOException ex) {
                throw new IllegalStateException(ex);
              }
            });

    boolean finished = process.waitFor(PROCESS_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
    if (!finished) {
      process.destroyForcibly();
      throw new IllegalStateException("CLI process timed out.");
    }

    String stdout = stdoutFuture.join();
    String stderr = stderrFuture.join();
    return new ProcessResult(process.exitValue(), stdout, stderr);
  }

  private static String javaCommand() {
    String javaHome = System.getProperty("java.home");
    assertNotNull(javaHome);
    Path javaBin = Path.of(javaHome, "bin", isWindows() ? "java.exe" : "java");
    return javaBin.toString();
  }

  private static Path createConfigFile(
      Path outputDir,
      String dbDriver,
      String dbUrl,
      String query,
      String outputFormats,
      boolean manifestEnabled,
      boolean usePasswordEnv,
      boolean allowEmptyPassword)
      throws IOException {
    Path config = Files.createTempFile("cli-e2e-config-", ".properties");
    StringBuilder content = new StringBuilder();
    content.append("db.driver=").append(dbDriver).append("\n");
    content.append("db.url=").append(dbUrl).append("\n");
    content.append("db.user=sa\n");
    if (usePasswordEnv) {
      content.append("db.passwordEnv=ZEUS_IBMI_DB_PASSWORD\n");
    }
    if (allowEmptyPassword) {
      content.append("db.allowEmptyPassword=true\n");
    }
    content.append("query.sql=").append(query).append("\n");
    content.append("output.directory=").append(asPortablePath(outputDir)).append("\n");
    content.append("output.formats=").append(outputFormats).append("\n");
    content.append("run.manifest.enabled=").append(manifestEnabled).append("\n");
    Files.writeString(config, content.toString(), StandardCharsets.UTF_8);
    return config;
  }

  private static Path createConfigFileWithQueryFile(
      Path outputDir,
      String dbDriver,
      String dbUrl,
      String queryFile,
      String outputFormats,
      boolean manifestEnabled,
      boolean usePasswordEnv,
      boolean allowEmptyPassword)
      throws IOException {
    Path config = Files.createTempFile("cli-e2e-config-query-file-", ".properties");
    StringBuilder content = new StringBuilder();
    content.append("db.driver=").append(dbDriver).append("\n");
    content.append("db.url=").append(dbUrl).append("\n");
    content.append("db.user=sa\n");
    if (usePasswordEnv) {
      content.append("db.passwordEnv=ZEUS_IBMI_DB_PASSWORD\n");
    }
    if (allowEmptyPassword) {
      content.append("db.allowEmptyPassword=true\n");
    }
    content.append("query.file=").append(queryFile).append("\n");
    content.append("output.directory=").append(asPortablePath(outputDir)).append("\n");
    content.append("output.formats=").append(outputFormats).append("\n");
    content.append("run.manifest.enabled=").append(manifestEnabled).append("\n");
    Files.writeString(config, content.toString(), StandardCharsets.UTF_8);
    return config;
  }

  private static List<Path> listFiles(Path directory) throws IOException {
    try (Stream<Path> stream = Files.list(directory)) {
      return stream.toList();
    }
  }

  private static boolean hasDataExtension(Path file, String... extensions) {
    String name = file.getFileName().toString();
    for (String extension : extensions) {
      if (name.endsWith("." + extension)) {
        return true;
      }
    }
    return false;
  }

  private static boolean isDataOutputFile(Path file, String... extensions) {
    String name = file.getFileName().toString();
    if (name.endsWith(".manifest.json")) {
      return false;
    }
    return hasDataExtension(file, extensions);
  }

  private static boolean isWindows() {
    return System.getProperty("os.name", "").toLowerCase().contains("win");
  }

  private static String asPortablePath(Path path) {
    return path.toAbsolutePath().toString().replace('\\', '/');
  }

  private record ProcessResult(int exitCode, String stdout, String stderr) {}
}
