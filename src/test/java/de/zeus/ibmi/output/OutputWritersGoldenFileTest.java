package de.zeus.ibmi.output;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.zeus.ibmi.transform.QueryResult;
import de.zeus.ibmi.transform.QueryResultFixture;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class OutputWritersGoldenFileTest {

    private final QueryResult sampleResult = QueryResultFixture.sampleResult();

    @Test
    void xmlWriter_shouldMatchGoldenFile() throws Exception {
        assertGolden(new XmlOutputWriter(), "/golden/output/sample.xml");
    }

    @Test
    void jsonWriter_shouldMatchGoldenFile() throws Exception {
        assertGolden(new JsonOutputWriter(), "/golden/output/sample.json");
    }

    @Test
    void csvWriter_shouldMatchGoldenFile() throws Exception {
        assertGolden(new CsvOutputWriter(), "/golden/output/sample.csv");
    }

    @Test
    void markdownWriter_shouldMatchGoldenFile() throws Exception {
        assertGolden(new MarkdownOutputWriter(), "/golden/output/sample.md");
    }

    private void assertGolden(OutputWriter writer, String resourcePath) throws Exception {
        String actual = writer.render(sampleResult);
        String expected = resourceAsString(resourcePath);
        assertEquals(expected, actual);
    }

    private static String resourceAsString(String resourcePath) throws IOException {
        try (InputStream in = OutputWritersGoldenFileTest.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
