package de.zeus.ibmi.output;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.zeus.ibmi.transform.QueryResultFixture;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class OutputExportServiceTest {

    @Test
    void writeAll_shouldCreateRequestedOutputFiles() throws Exception {
        OutputExportService service = new OutputExportService(OutputWriters.defaultWriters());
        Path outputDir = Files.createTempDirectory("zeus-ibmi-out-");

        List<Path> files = service.writeAll(
                QueryResultFixture.sampleResult(),
                outputDir,
                "test-export",
                List.of("xml", "json", "csv", "md"));

        assertEquals(4, files.size());
        assertTrue(Files.exists(outputDir.resolve("test-export.xml")));
        assertTrue(Files.exists(outputDir.resolve("test-export.json")));
        assertTrue(Files.exists(outputDir.resolve("test-export.csv")));
        assertTrue(Files.exists(outputDir.resolve("test-export.md")));
    }
}
