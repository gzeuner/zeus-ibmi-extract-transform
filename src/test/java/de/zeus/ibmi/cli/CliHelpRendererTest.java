package de.zeus.ibmi.cli;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CliHelpRendererTest {

    @Test
    void render_shouldContainToolVersionAndCoreSections() {
        String help = new CliHelpRenderer().render("zeus-ibmi-extract-transform", "1.2.3");

        assertTrue(help.contains("zeus-ibmi-extract-transform 1.2.3"));
        assertTrue(help.contains("Usage:"));
        assertTrue(help.contains("General:"));
        assertTrue(help.contains("Config:"));
        assertTrue(help.contains("Database:"));
        assertTrue(help.contains("Query:"));
        assertTrue(help.contains("Output:"));
        assertTrue(help.contains("Execution:"));
        assertTrue(help.contains("--db-password"));
        assertTrue(help.contains("--query-file <file>"));
        assertTrue(help.contains("Prefer ENV-based secrets"));
        assertTrue(help.contains("Exit codes:"));
    }
}
