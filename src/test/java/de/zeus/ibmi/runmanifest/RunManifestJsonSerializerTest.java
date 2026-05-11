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
                List.of(
                        new OutputFileMetadata(
                                "xml",
                                "out/export.xml",
                                "export.xml",
                                12L,
                                "sha256:111",
                                Instant.parse("2026-05-11T06:00:02Z")),
                        new OutputFileMetadata(
                                "json",
                                "out/export.json",
                                "export.json",
                                24L,
                                "sha256:222",
                                Instant.parse("2026-05-11T06:00:03Z"))),
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
        assertTrue(json.contains("\"outputDirectory\":\"./output\""));
        assertTrue(json.contains("\"outputFiles\":[{"));
        assertTrue(json.contains("\"format\":\"xml\""));
        assertTrue(json.contains("\"path\":\"out/export.xml\""));
        assertTrue(json.contains("\"fileName\":\"export.xml\""));
        assertTrue(json.contains("\"sizeBytes\":12"));
        assertTrue(json.contains("\"sha256\":\"sha256:111\""));
        assertTrue(json.contains("\"outputFormats\":[\"xml\",\"json\"]"));
        assertTrue(json.contains("\"rowCount\":10"));
        assertTrue(json.contains("\"columnCount\":2"));
        assertTrue(json.contains("\"errorClass\":\"\""));
    }

    @Test
    void toJson_shouldNotContainAbsoluteLocalPathsWhenOutputDirectoryIsRedacted() {
        RunManifest manifest = new RunManifest(
                "zeus-ibmi-extract-transform",
                "0.1.0-SNAPSHOT",
                "run-002",
                "SUCCESS",
                false,
                Instant.parse("2026-05-11T06:00:00Z"),
                Instant.parse("2026-05-11T06:00:01Z"),
                1000L,
                "config/example.application.properties",
                "sha256:abc123456",
                "SELECT 1",
                "<output-directory>",
                List.of(
                        new OutputFileMetadata(
                                "jsonl",
                                "run-002.jsonl",
                                "run-002.jsonl",
                                88L,
                                "sha256:abcd",
                                Instant.parse("2026-05-11T06:00:01Z"))),
                List.of("jsonl"),
                1,
                1,
                "",
                "",
                "17",
                "Linux",
                "6.x");

        String json = RunManifestJsonSerializer.toJson(manifest);

        assertTrue(json.contains("\"outputDirectory\":\"<output-directory>\""));
        assertTrue(json.contains("\"fileName\":\"run-002.jsonl\""));
        assertTrue(json.contains("\"sizeBytes\":88"));
        assertTrue(json.contains("\"sha256\":\"sha256:abcd\""));
        assertTrue(!json.contains("/home/"));
        assertTrue(!json.contains("C:\\\\Users"));
    }
}
