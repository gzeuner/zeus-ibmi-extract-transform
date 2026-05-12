package de.zeus.ibmi.runmanifest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class RunManifestFactoryTest {

  @Test
  void success_shouldSetDurationAndOutputFiles() throws Exception {
    Path outputDir = Files.createTempDirectory("manifest-factory-success-");
    Path xmlFile = outputDir.resolve("out.xml");
    Path jsonFile = outputDir.resolve("out.json");
    Files.writeString(xmlFile, "<x/>", StandardCharsets.UTF_8);
    Files.writeString(jsonFile, "{}", StandardCharsets.UTF_8);

    Instant start = Instant.parse("2026-05-11T06:00:00Z");
    Instant finish = Instant.parse("2026-05-11T06:00:02Z");
    RunManifest manifest =
        RunManifestFactory.success(
            "tool-a",
            "1.0.0",
            "run-1",
            start,
            finish,
            "config.properties",
            "CLI_INLINE",
            "CLI inline",
            "sha256:x",
            "SELECT 1",
            outputDir,
            List.of(xmlFile, jsonFile),
            List.of("xml", "json"),
            3,
            2);

    assertEquals("SUCCESS", manifest.status());
    assertEquals("tool-a", manifest.toolName());
    assertEquals("1.0.0", manifest.toolVersion());
    assertEquals("<output-directory>", manifest.outputDirectory());
    assertEquals(2000L, manifest.durationMillis());
    assertEquals(2, manifest.outputFiles().size());
    assertEquals("out.xml", manifest.outputFiles().get(0).path());
    assertEquals("xml", manifest.outputFiles().get(0).format());
    assertTrue(manifest.outputFiles().get(0).sha256().startsWith("sha256:"));
    assertTrue(manifest.outputFiles().get(0).sizeBytes() > 0L);
    assertEquals(2, manifest.outputFormats().size());
    assertEquals(3, manifest.rowCount());
    assertEquals(2, manifest.columnCount());
  }

  @Test
  void failure_shouldSetErrorClass() {
    Instant start = Instant.parse("2026-05-11T06:00:00Z");
    Instant finish = Instant.parse("2026-05-11T06:00:01Z");
    RunManifest manifest =
        RunManifestFactory.failure(
            "tool-a",
            "1.0.0",
            "run-2",
            start,
            finish,
            "config.properties",
            "CONFIG_FILE",
            "/home/user/queries/demo.sql",
            "sha256:x",
            "SELECT 1",
            Path.of("./output"),
            List.of(),
            List.of("xml"),
            new IllegalStateException("x"),
            "password=secret123");

    assertEquals("FAILED", manifest.status());
    assertTrue(manifest.errorClass().contains("IllegalStateException"));
    assertEquals("password=***", manifest.errorMessage());
    assertEquals("./output", manifest.outputDirectory());
    assertEquals(1000L, manifest.durationMillis());
  }

  @Test
  void dryRun_shouldSetDryRunStatusAndNoOutputFiles() {
    Instant start = Instant.parse("2026-05-11T06:00:00Z");
    Instant finish = Instant.parse("2026-05-11T06:00:00Z");
    RunManifest manifest =
        RunManifestFactory.dryRun(
            "tool-a",
            "1.0.0",
            "run-3",
            start,
            finish,
            "config.properties",
            "CONFIG_INLINE",
            "query.sql",
            "sha256:y",
            "SELECT 1",
            "./output",
            List.of("xml", "json"));

    assertEquals("DRY_RUN", manifest.status());
    assertTrue(manifest.dryRun());
    assertTrue(manifest.outputFiles().isEmpty());
    assertEquals("./output", manifest.outputDirectory());
    assertEquals(2, manifest.outputFormats().size());
  }

  @Test
  void dryRun_shouldRedactAbsoluteOutputDirectory() {
    Instant start = Instant.parse("2026-05-11T06:00:00Z");
    Instant finish = Instant.parse("2026-05-11T06:00:00Z");
    RunManifest manifest =
        RunManifestFactory.dryRun(
            "tool-a",
            "1.0.0",
            "run-4",
            start,
            finish,
            "config.properties",
            "CONFIG_FILE",
            "/home/example/query.sql",
            "sha256:y",
            "SELECT 1",
            "/home/example/output",
            List.of("xml"));

    assertEquals("<output-directory>", manifest.outputDirectory());
    assertEquals("query.sql", manifest.querySource());
  }

  @Test
  void success_shouldRedactAbsoluteQuerySourceToFileName() throws Exception {
    Path outputDir = Files.createTempDirectory("manifest-factory-query-source-");
    Path jsonFile = outputDir.resolve("out.json");
    Files.writeString(jsonFile, "{}", StandardCharsets.UTF_8);

    RunManifest manifest =
        RunManifestFactory.success(
            "tool-a",
            "1.0.0",
            "run-5",
            Instant.parse("2026-05-11T06:00:00Z"),
            Instant.parse("2026-05-11T06:00:01Z"),
            "config.properties",
            "CLI_FILE",
            "/home/secret/path/customers.sql",
            "sha256:z",
            "SELECT 1",
            outputDir,
            List.of(jsonFile),
            List.of("json"),
            1,
            1);

    assertEquals("customers.sql", manifest.querySource());
  }
}
