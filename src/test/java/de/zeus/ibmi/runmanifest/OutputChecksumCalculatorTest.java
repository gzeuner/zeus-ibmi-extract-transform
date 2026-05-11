package de.zeus.ibmi.runmanifest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class OutputChecksumCalculatorTest {

    @Test
    void sha256_shouldBeStableForSameFileContent() throws Exception {
        Path file = Files.createTempFile("sha256-test-", ".txt");
        Files.writeString(file, "abc", StandardCharsets.UTF_8);

        String first = OutputChecksumCalculator.sha256(file);
        String second = OutputChecksumCalculator.sha256(file);

        assertEquals("sha256:ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", first);
        assertEquals(first, second);
    }
}
