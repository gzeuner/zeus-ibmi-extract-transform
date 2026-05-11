package de.zeus.ibmi.runmanifest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ManifestPathFormatterTest {

    @Test
    void outputDirectoryDisplay_shouldRedactAbsoluteLinuxPath() {
        assertEquals(
                ManifestPathFormatter.REDACTED_OUTPUT_DIRECTORY,
                ManifestPathFormatter.outputDirectoryDisplay("/home/dev/output"));
    }

    @Test
    void outputDirectoryDisplay_shouldRedactAbsoluteWindowsPath() {
        assertEquals(
                ManifestPathFormatter.REDACTED_OUTPUT_DIRECTORY,
                ManifestPathFormatter.outputDirectoryDisplay("C:\\Users\\alice\\out"));
    }

    @Test
    void outputDirectoryDisplay_shouldKeepRelativePathReadable() {
        assertEquals("build/output", ManifestPathFormatter.outputDirectoryDisplay("build\\output"));
    }

    @Test
    void outputFilePathDisplay_shouldReturnRelativePathWhenInsideOutputDirectory() {
        Path outputDir = Path.of("/tmp/run-1/output");
        Path outputFile = outputDir.resolve("nested").resolve("data.jsonl");
        assertEquals("nested/data.jsonl", ManifestPathFormatter.outputFilePathDisplay(outputDir, outputFile));
    }

    @Test
    void outputFilePathDisplay_shouldFallbackToFileNameWhenOutsideOutputDirectory() {
        Path outputDir = Path.of("/tmp/run-1/output");
        Path outputFile = Path.of("/tmp/another/place/data.json");
        assertEquals("data.json", ManifestPathFormatter.outputFilePathDisplay(outputDir, outputFile));
    }
}
