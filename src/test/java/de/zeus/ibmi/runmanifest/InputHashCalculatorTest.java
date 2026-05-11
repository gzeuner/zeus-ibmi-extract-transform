package de.zeus.ibmi.runmanifest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.zeus.ibmi.config.AppConfig;
import de.zeus.ibmi.config.OutputFormat;
import java.util.List;
import org.junit.jupiter.api.Test;

class InputHashCalculatorTest {

    @Test
    void sha256For_shouldBeStableForSameInput() {
        AppConfig config = new AppConfig(
                "org.h2.Driver",
                "jdbc:h2:mem:test",
                "sa",
                "",
                null,
                "SELECT * FROM TEST_DATA",
                null,
                "./output",
                List.of(OutputFormat.XML, OutputFormat.JSON),
                true,
                null,
                null,
                true);

        String hash1 = InputHashCalculator.sha256For(config, "SELECT * FROM TEST_DATA");
        String hash2 = InputHashCalculator.sha256For(config, "SELECT * FROM TEST_DATA");

        assertEquals(hash1, hash2);
        assertTrue(hash1.startsWith("sha256:"));
    }

    @Test
    void sha256For_shouldChangeWhenInputChanges() {
        AppConfig config = new AppConfig(
                "org.h2.Driver",
                "jdbc:h2:mem:test",
                "sa",
                "",
                null,
                "SELECT * FROM TEST_DATA",
                null,
                "./output",
                List.of(OutputFormat.XML, OutputFormat.JSON),
                true,
                null,
                null,
                true);

        String hash1 = InputHashCalculator.sha256For(config, "SELECT * FROM TEST_DATA");
        String hash2 = InputHashCalculator.sha256For(config, "SELECT ID FROM TEST_DATA");

        assertNotEquals(hash1, hash2);
    }
}
