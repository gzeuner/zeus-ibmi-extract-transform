package de.zeus.ibmi.runmanifest;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class RunManifestWriterTest {

  @Test
  void write_shouldPersistJsonFile() throws Exception {
    RunManifest manifest =
        RunManifestFactory.dryRun(
            "zeus-ibmi-extract-transform",
            "0.1.0-SNAPSHOT",
            "run-123",
            Instant.parse("2026-05-11T06:00:00Z"),
            Instant.parse("2026-05-11T06:00:00Z"),
            "config/example.application.properties",
            "CONFIG_INLINE",
            "query.sql",
            "sha256:abc",
            "SELECT 1",
            "./output",
            List.of("xml"));

    Path outDir = Files.createTempDirectory("manifest-write-test-");
    Path manifestFile = new RunManifestWriter().write(outDir, manifest);

    assertTrue(Files.exists(manifestFile));
    String json = Files.readString(manifestFile);
    assertTrue(json.contains("\"runId\":\"run-123\""));
    assertTrue(json.contains("\"status\":\"DRY_RUN\""));
  }
}
