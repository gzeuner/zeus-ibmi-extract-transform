package de.zeus.ibmi.runmanifest;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class RunManifestJsonSerializerTest {

    @Test
    void toJson_shouldSerializeCoreFields() {
        RunManifest manifest = new RunManifest(
                "zeus-ibmi-extract-transform",
                "0.1.0-SNAPSHOT",
                "run-001",
                "SUCCESS",
                false,
                Instant.parse("2026-05-11T06:00:00Z"),
                Instant.parse("2026-05-11T06:00:05Z"),
                5000L,
                "config/example.application.properties",
                "sha256:abc123456",
                "SELECT 1",
                "./output",
                List.of("out/export.xml", "out/export.json"),
                List.of("xml", "json"),
                10,
                2,
                "",
                "",
                "17",
                "Linux",
                "6.x");

        String json = RunManifestJsonSerializer.toJson(manifest);

        assertTrue(json.startsWith("{"));
        assertTrue(json.contains("\"toolName\":\"zeus-ibmi-extract-transform\""));
        assertTrue(json.contains("\"runId\":\"run-001\""));
        assertTrue(json.contains("\"status\":\"SUCCESS\""));
        assertTrue(json.contains("\"dryRun\":false"));
        assertTrue(json.contains("\"durationMillis\":5000"));
        assertTrue(json.contains("\"queryHash\":\"sha256:abc123456\""));
        assertTrue(json.contains("\"queryPreview\":\"SELECT 1\""));
        assertTrue(json.contains("\"outputFiles\":[\"out/export.xml\",\"out/export.json\"]"));
        assertTrue(json.contains("\"outputFormats\":[\"xml\",\"json\"]"));
        assertTrue(json.contains("\"rowCount\":10"));
        assertTrue(json.contains("\"columnCount\":2"));
        assertTrue(json.contains("\"errorClass\":\"\""));
    }
}
