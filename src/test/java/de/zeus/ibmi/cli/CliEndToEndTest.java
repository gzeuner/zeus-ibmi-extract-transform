package de.zeus.ibmi.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.zeus.ibmi.version.VersionProvider;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
        Path config = createConfigFile(
                outputDir,
                "org.h2.Driver",
                "jdbc:h2:mem:cli_e2e_dryrun;MODE=DB2;DB_CLOSE_DELAY=-1",
                "SELECT 1 AS X",
                "xml,jsonl",
                true,
                true,
                true);

        ProcessResult result = runCli(
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
        assertTrue(files.stream().anyMatch(path -> path.getFileName().toString().endsWith(".manifest.json")));
        assertFalse(files.stream().anyMatch(path -> isDataOutputFile(path, "xml", "json", "jsonl", "csv", "md")));
    }

    @Test
    void execute_againstH2_shouldWriteOutputsAndSuccessManifestWithChecksums() throws Exception {
        Path outputDir = Files.createTempDirectory("cli-e2e-exec-out-");
        Path config = createConfigFile(
                outputDir,
                "org.h2.Driver",
                "jdbc:h2:mem:cli_e2e_execute;MODE=DB2;DB_CLOSE_DELAY=-1",
                "SELECT 1 AS ID, 'Müller' AS NAME, CAST(NULL AS VARCHAR) AS NOTES UNION ALL SELECT 2 AS ID, 'Bob|Builder;\"X\"' AS NAME, 'Line1' || CHAR(10) || 'Line2' AS NOTES ORDER BY ID",
                "xml,json,csv,md,jsonl",
                true,
                true,
                true);

        ProcessResult result = runCli(
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

        Path manifest = files.stream()
                .filter(path -> path.getFileName().toString().endsWith(".manifest.json"))
                .max(Comparator.naturalOrder())
                .orElseThrow();
        String manifestJson = Files.readString(manifest, StandardCharsets.UTF_8);
        assertTrue(manifestJson.contains("\"outputDirectory\":\"<output-directory>\""));
        assertFalse(manifestJson.contains(outputDir.toString().replace('\\', '/')));
        assertTrue(manifestJson.contains("\"status\":\"SUCCESS\""));
        assertTrue(manifestJson.contains("\"toolVersion\":\"" + VersionProvider.DEVELOPMENT_FALLBACK_VERSION + "\""));
        assertTrue(manifestJson.contains("\"rowCount\":2"));
        assertTrue(manifestJson.contains("\"format\":\"jsonl\""));
        assertTrue(manifestJson.contains("\"sizeBytes\":"));
        assertTrue(manifestJson.contains("\"sha256\":\"sha256:"));
    }

    @Test
    void invalidQuery_shouldReturnGuardExitCodeAndNoExecution() throws Exception {
        Path outputDir = Files.createTempDirectory("cli-e2e-guard-out-");
        Path config = createConfigFile(
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
        Path config = createConfigFile(
                outputDir,
                "example.DoesNotExistDriver",
                "jdbc:h2:mem:cli_e2e_jdbc_fail;MODE=DB2;DB_CLOSE_DELAY=-1",
                "SELECT 1 AS X",
                "json,jsonl",
                true,
                true,
                true);

        ProcessResult result = runCli(
                List.of("--config", config.toString(), "--execute"),
                Map.of("ZEUS_IBMI_DB_PASSWORD", "dummy-secret-jdbc"));

        assertEquals(4, result.exitCode());
        assertTrue(result.stdout().contains("Status: FAILED"));
        assertFalse(result.stdout().contains("dummy-secret-jdbc"));
        assertFalse(result.stderr().contains("dummy-secret-jdbc"));

        Path manifest = listFiles(outputDir).stream()
                .filter(path -> path.getFileName().toString().endsWith(".manifest.json"))
                .max(Comparator.naturalOrder())
                .orElseThrow();
        String manifestJson = Files.readString(manifest, StandardCharsets.UTF_8);
        assertTrue(manifestJson.contains("\"status\":\"FAILED\""));
        assertTrue(manifestJson.contains("\"errorClass\":\"de.zeus.ibmi.selection.QueryExecutionException\""));
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

    private static ProcessResult runCli(List<String> args, Map<String, String> envOverrides) throws Exception {
        List<String> command = new ArrayList<>();
        command.add(javaCommand());
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add("de.zeus.ibmi.cli.Main");
        command.addAll(args);

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.environment().putAll(envOverrides);

        Process process = processBuilder.start();
        boolean finished = process.waitFor(PROCESS_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IllegalStateException("CLI process timed out.");
        }

        String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
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
            boolean allowEmptyPassword) throws IOException {
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
        content.append("output.directory=").append(outputDir).append("\n");
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

    private record ProcessResult(int exitCode, String stdout, String stderr) {
    }
}
