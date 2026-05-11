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
        RunManifest manifest = RunManifestFactory.success(
                "tool-a",
                "1.0.0",
                "run-1",
                start,
                finish,
                "config.properties",
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
        RunManifest manifest = RunManifestFactory.failure(
                "tool-a",
                "1.0.0",
                "run-2",
                start,
                finish,
                "config.properties",
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
        assertEquals(1000L, manifest.durationMillis());
    }

    @Test
    void dryRun_shouldSetDryRunStatusAndNoOutputFiles() {
        Instant start = Instant.parse("2026-05-11T06:00:00Z");
        Instant finish = Instant.parse("2026-05-11T06:00:00Z");
        RunManifest manifest = RunManifestFactory.dryRun(
                "tool-a",
                "1.0.0",
                "run-3",
                start,
                finish,
                "config.properties",
                "sha256:y",
                "SELECT 1",
                "./output",
                List.of("xml", "json"));

        assertEquals("DRY_RUN", manifest.status());
        assertTrue(manifest.dryRun());
        assertTrue(manifest.outputFiles().isEmpty());
        assertEquals(2, manifest.outputFormats().size());
    }
}
