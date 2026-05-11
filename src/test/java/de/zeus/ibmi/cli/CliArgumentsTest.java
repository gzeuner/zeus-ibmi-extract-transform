package de.zeus.ibmi.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CliArgumentsTest {

    @Test
    void parse_shouldReadFlagsAndOverrides() {
        CliArguments args = CliArguments.parse(new String[] {
                "--config", "app.properties",
                "--execute",
                "--db-url", "jdbc:h2:mem:test",
                "--output-formats", "json,csv",
                "--manifest-disabled"
        });

        assertEquals("app.properties", args.configPath().toString());
        assertTrue(args.execute());
        assertEquals("jdbc:h2:mem:test", args.configOverrides().get("db.url"));
        assertEquals("json,csv", args.configOverrides().get("output.formats"));
        assertEquals("false", args.configOverrides().get("run.manifest.enabled"));
    }
}
