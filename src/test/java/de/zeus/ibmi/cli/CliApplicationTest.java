package de.zeus.ibmi.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.zeus.ibmi.version.VersionProvider;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CliApplicationTest {

    @Test
    void run_shouldPrintHelpWhenNoArgs() {
        CliApplication app = new CliApplication(Map.of());
        ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
        ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();

        int exit = app.run(new String[0], printStream(outBuffer), printStream(errBuffer));

        assertEquals(0, exit);
        assertTrue(outBuffer.toString(StandardCharsets.UTF_8).contains("Usage:"));
    }

    @Test
    void run_shouldPrintHelpWithHelpFlags() {
        CliApplication app = new CliApplication(Map.of());
        ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
        ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();

        int exitLong = app.run(new String[] { "--help" }, printStream(outBuffer), printStream(errBuffer));
        int exitShort = app.run(new String[] { "-h" }, printStream(outBuffer), printStream(errBuffer));

        assertEquals(0, exitLong);
        assertEquals(0, exitShort);
        assertTrue(outBuffer.toString(StandardCharsets.UTF_8).contains("Usage:"));
    }

    @Test
    void run_shouldPrintVersion() {
        CliApplication app = new CliApplication(Map.of());
        ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
        ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();

        int exit = app.run(new String[] { "--version" }, printStream(outBuffer), printStream(errBuffer));

        assertEquals(0, exit);
        assertTrue(outBuffer.toString(StandardCharsets.UTF_8).contains(VersionProvider.DEVELOPMENT_FALLBACK_VERSION));
    }

    @Test
    void run_shouldPrintInjectedVersion() {
        VersionProvider provider = new VersionProvider(
                () -> "1.2.3",
                () -> "9.9.9",
                VersionProvider.DEVELOPMENT_FALLBACK_VERSION);
        CliApplication app = new CliApplication(Map.of(), provider);
        ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
        ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();

        int exit = app.run(new String[] { "--version" }, printStream(outBuffer), printStream(errBuffer));

        assertEquals(0, exit);
        assertTrue(outBuffer.toString(StandardCharsets.UTF_8).contains("1.2.3"));
    }

    @Test
    void run_shouldFailWithConfigErrorWhenConfigMissing() {
        CliApplication app = new CliApplication(Map.of());
        ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
        ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();

        int exit = app.run(new String[] { "--execute" }, printStream(outBuffer), printStream(errBuffer));

        assertEquals(2, exit);
        assertTrue(errBuffer.toString(StandardCharsets.UTF_8).contains("Missing required argument"));
    }

    @Test
    void run_shouldFailWithConfigErrorForUnknownOption() {
        CliApplication app = new CliApplication(Map.of());
        ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
        ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();

        int exit = app.run(new String[] { "--unknown" }, printStream(outBuffer), printStream(errBuffer));

        assertEquals(2, exit);
        assertTrue(errBuffer.toString(StandardCharsets.UTF_8).contains("Unknown argument: --unknown"));
    }

    @Test
    void run_shouldFailWithConfigErrorForMissingOptionValue() {
        CliApplication app = new CliApplication(Map.of());
        ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
        ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();

        int exit = app.run(new String[] { "--config" }, printStream(outBuffer), printStream(errBuffer));

        assertEquals(2, exit);
        assertTrue(errBuffer.toString(StandardCharsets.UTF_8).contains("Missing value for --config"));
    }

    @Test
    void run_shouldPerformDryRunByDefault() throws Exception {
        Path config = createConfigFile();
        CliApplication app = new CliApplication(Map.of("ZEUS_IBMI_DB_PASSWORD", "dummy"));
        ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
        ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();

        int exit = app.run(new String[] { "--config", config.toString() }, printStream(outBuffer), printStream(errBuffer));

        assertEquals(0, exit);
        String out = outBuffer.toString(StandardCharsets.UTF_8);
        assertTrue(out.contains("Mode: DRY-RUN"));
        assertTrue(out.contains("Status: DRY_RUN"));
        assertTrue(out.contains("Read-only query check: OK"));
        assertTrue(out.contains("Hint: Add --execute to run read-only query execution."));
    }

    @Test
    void run_shouldTriggerExecuteOnlyWithFlag() throws Exception {
        Path config = createConfigFile();
        CliApplication app = new CliApplication(Map.of("ZEUS_IBMI_DB_PASSWORD", "dummy"));
        ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
        ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();

        int exit = app.run(
                new String[] { "--config", config.toString(), "--execute" },
                printStream(outBuffer),
                printStream(errBuffer));

        assertEquals(0, exit);
        assertTrue(outBuffer.toString(StandardCharsets.UTF_8).contains("Mode: EXECUTE"));
        assertTrue(outBuffer.toString(StandardCharsets.UTF_8).contains("Status: SUCCESS"));
        assertTrue(outBuffer.toString(StandardCharsets.UTF_8).contains("Row Count: 1"));
        assertTrue(outBuffer.toString(StandardCharsets.UTF_8).contains("\"status\":\"SUCCESS\""));
        assertTrue(outBuffer.toString(StandardCharsets.UTF_8)
                .contains("\"toolVersion\":\"" + VersionProvider.DEVELOPMENT_FALLBACK_VERSION + "\""));
    }

    @Test
    void run_shouldReturnJdbcExitCodeWhenExecutionFails() throws Exception {
        Path outputDir = Files.createTempDirectory("cli-out-invalid-driver-check-");
        Path config = createConfigFileWithInvalidDriver(outputDir);
        CliApplication app = new CliApplication(Map.of("ZEUS_IBMI_DB_PASSWORD", "dummy"));
        ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
        ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();

        int exit = app.run(
                new String[] { "--config", config.toString(), "--execute" },
                printStream(outBuffer),
                printStream(errBuffer));

        assertEquals(4, exit);
        assertTrue(outBuffer.toString(StandardCharsets.UTF_8).contains("Status: FAILED"));
        assertTrue(outBuffer.toString(StandardCharsets.UTF_8).contains("\"status\":\"FAILED\""));
        assertTrue(outBuffer.toString(StandardCharsets.UTF_8).contains("\"errorClass\":\"de.zeus.ibmi.selection.QueryExecutionException\""));
        Path manifest = Files.list(outputDir)
                .filter(path -> path.getFileName().toString().endsWith(".manifest.json"))
                .max(Comparator.naturalOrder())
                .orElseThrow();
        assertTrue(Files.readString(manifest, StandardCharsets.UTF_8).contains("\"status\":\"FAILED\""));
    }

    @Test
    void run_shouldNotPrintSecretPlaintext() throws Exception {
        Path config = createConfigFile();
        CliApplication app = new CliApplication(Map.of("ZEUS_IBMI_DB_PASSWORD", "secret123"));
        ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
        ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();

        app.run(new String[] { "--config", config.toString() }, printStream(outBuffer), printStream(errBuffer));

        String out = outBuffer.toString(StandardCharsets.UTF_8);
        String err = errBuffer.toString(StandardCharsets.UTF_8);
        assertFalse(out.contains("secret123"));
        assertFalse(err.contains("secret123"));
    }

    private static Path createConfigFile() throws Exception {
        Path outputDir = Files.createTempDirectory("cli-out-");
        Path file = Files.createTempFile("cli-config-", ".properties");
        Files.writeString(file, ""
                + "db.driver=org.h2.Driver\n"
                + "db.url=jdbc:h2:mem:cli;MODE=DB2;DB_CLOSE_DELAY=-1\n"
                + "db.user=sa\n"
                + "db.passwordEnv=ZEUS_IBMI_DB_PASSWORD\n"
                + "query.sql=SELECT 1 AS X\n"
                + "output.directory=" + outputDir + "\n"
                + "output.formats=json\n"
                + "run.manifest.enabled=true\n");
        return file;
    }

    private static Path createConfigFileWithInvalidDriver(Path outputDir) throws Exception {
        Path file = Files.createTempFile("cli-config-invalid-driver-", ".properties");
        Files.writeString(file, ""
                + "db.driver=example.DoesNotExistDriver\n"
                + "db.url=jdbc:h2:mem:cli_invalid;MODE=DB2;DB_CLOSE_DELAY=-1\n"
                + "db.user=sa\n"
                + "db.passwordEnv=ZEUS_IBMI_DB_PASSWORD\n"
                + "query.sql=SELECT 1 AS X\n"
                + "output.directory=" + outputDir + "\n"
                + "output.formats=json\n"
                + "run.manifest.enabled=true\n");
        return file;
    }

    private static PrintStream printStream(ByteArrayOutputStream buffer) {
        return new PrintStream(buffer, true, StandardCharsets.UTF_8);
    }
}
