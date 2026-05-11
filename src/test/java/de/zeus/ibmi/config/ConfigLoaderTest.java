package de.zeus.ibmi.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ConfigLoaderTest {

    @Test
    void load_shouldApplyEnvOverrides() throws Exception {
        Path outputDir = Files.createTempDirectory("cfg-out-");
        Path file = Files.createTempFile("zeus-ibmi-config-", ".properties");
        Files.writeString(file, ""
                + "db.driver=com.ibm.as400.access.AS400JDBCDriver\n"
                + "db.url=jdbc:as400://host-a\n"
                + "db.user=user-a\n"
                + "db.password=password-a\n"
                + "query.sql=select * from qsys2.systables\n"
                + "output.formats=xml,json\n"
                + "output.directory=" + outputDir + "\n", StandardCharsets.UTF_8);

        AppConfig config = ConfigLoader.load(file, Map.of(
                "ZEUS_IBMI_DB_URL", "jdbc:as400://host-b",
                "ZEUS_IBMI_OUTPUT_FORMATS", "csv,md"));

        assertEquals("jdbc:as400://host-b", config.databaseUrl());
        assertEquals("user-a", config.username());
        assertEquals(2, config.outputFormats().size());
        assertEquals(OutputFormat.CSV, config.outputFormats().get(0));
        assertEquals(OutputFormat.MD, config.outputFormats().get(1));
    }

    @Test
    void loadExample_shouldLoadFromClasspath() throws Exception {
        AppConfig config = ConfigLoader.loadExample(Map.of("ZEUS_IBMI_DB_PASSWORD", "dummy"));

        assertNotNull(config);
        assertNotNull(config.databaseUrl());
        assertTrue(config.query().toLowerCase().contains("select"));
    }

    @Test
    void load_shouldFailWhenRequiredValuesAreMissing() throws Exception {
        Path file = Files.createTempFile("zeus-ibmi-config-invalid-", ".properties");
        Files.writeString(file, "db.url=jdbc:as400://host\n", StandardCharsets.UTF_8);

        assertThrows(ConfigValidationException.class, () -> ConfigLoader.load(file, Map.of()));
    }

    @Test
    void load_shouldApplyCliOverridesOverEnvAndFile() throws Exception {
        Path outputDir = Files.createTempDirectory("cfg-cli-out-");
        Path file = Files.createTempFile("zeus-ibmi-config-cli-", ".properties");
        Files.writeString(file, ""
                + "db.driver=org.h2.Driver\n"
                + "db.url=jdbc:h2:mem:file\n"
                + "db.user=file-user\n"
                + "db.password=file-password\n"
                + "query.sql=SELECT 1\n"
                + "output.formats=xml\n"
                + "output.directory=" + outputDir + "\n", StandardCharsets.UTF_8);

        AppConfig config = ConfigLoader.load(
                file,
                Map.of("ZEUS_IBMI_DB_USER", "env-user"),
                Map.of("db.user", "cli-user", "output.formats", "json,csv"));

        assertEquals("cli-user", config.username());
        assertEquals(2, config.outputFormats().size());
        assertEquals(OutputFormat.JSON, config.outputFormats().get(0));
        assertEquals(OutputFormat.CSV, config.outputFormats().get(1));
    }

    @Test
    void load_shouldRejectInvalidOutputFormat() throws Exception {
        Path outputDir = Files.createTempDirectory("cfg-invalid-format-out-");
        Path file = Files.createTempFile("zeus-ibmi-config-invalid-format-", ".properties");
        Files.writeString(file, ""
                + "db.driver=org.h2.Driver\n"
                + "db.url=jdbc:h2:mem:test\n"
                + "db.user=sa\n"
                + "db.allowEmptyPassword=true\n"
                + "query.sql=SELECT 1\n"
                + "output.formats=xml,banana\n"
                + "output.directory=" + outputDir + "\n", StandardCharsets.UTF_8);

        assertThrows(ConfigValidationException.class, () -> ConfigLoader.load(file, Map.of()));
    }

    @Test
    void load_shouldAcceptJsonlOutputFormat() throws Exception {
        Path outputDir = Files.createTempDirectory("cfg-jsonl-format-out-");
        Path file = Files.createTempFile("zeus-ibmi-config-jsonl-format-", ".properties");
        Files.writeString(file, ""
                + "db.driver=org.h2.Driver\n"
                + "db.url=jdbc:h2:mem:test_jsonl\n"
                + "db.user=sa\n"
                + "db.allowEmptyPassword=true\n"
                + "query.sql=SELECT 1\n"
                + "output.formats=jsonl,json\n"
                + "output.directory=" + outputDir + "\n", StandardCharsets.UTF_8);

        AppConfig config = ConfigLoader.load(file, Map.of());
        assertEquals(2, config.outputFormats().size());
        assertEquals(OutputFormat.JSONL, config.outputFormats().get(0));
        assertEquals(OutputFormat.JSON, config.outputFormats().get(1));
    }

    @Test
    void load_shouldFailWhenOutputDirectoryCannotBeCreated() throws Exception {
        Path existingFile = Files.createTempFile("cfg-not-a-directory-", ".tmp");
        Path file = Files.createTempFile("zeus-ibmi-config-dir-", ".properties");
        Files.writeString(file, ""
                + "db.driver=org.h2.Driver\n"
                + "db.url=jdbc:h2:mem:test\n"
                + "db.user=sa\n"
                + "db.allowEmptyPassword=true\n"
                + "query.sql=SELECT 1\n"
                + "output.formats=xml\n"
                + "output.directory=" + existingFile + "\n", StandardCharsets.UTF_8);

        ConfigValidationException ex = assertThrows(ConfigValidationException.class, () -> ConfigLoader.load(file, Map.of()));
        assertTrue(ex.getMessage().contains("Cannot create output directory"));
    }

    @Test
    void load_shouldFailWhenPasswordMissingForNonH2WithoutOverride() throws Exception {
        Path outputDir = Files.createTempDirectory("cfg-no-pwd-");
        Path file = Files.createTempFile("zeus-ibmi-config-no-pwd-", ".properties");
        Files.writeString(file, ""
                + "db.driver=com.ibm.as400.access.AS400JDBCDriver\n"
                + "db.url=jdbc:as400://host-a\n"
                + "db.user=user-a\n"
                + "query.sql=SELECT * FROM qsys2.systables\n"
                + "output.formats=xml\n"
                + "output.directory=" + outputDir + "\n", StandardCharsets.UTF_8);

        ConfigValidationException ex = assertThrows(ConfigValidationException.class, () -> ConfigLoader.load(file, Map.of()));
        assertTrue(ex.getMessage().contains("db.password is required"));
    }
}
