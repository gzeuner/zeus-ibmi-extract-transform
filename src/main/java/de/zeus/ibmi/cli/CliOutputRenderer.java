package de.zeus.ibmi.cli;

import de.zeus.ibmi.runmanifest.OutputFileMetadata;
import de.zeus.ibmi.runmanifest.RunManifest;
import de.zeus.ibmi.security.SecretMasker;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;

public final class CliOutputRenderer {

    public void renderExecutionPlan(PrintStream out, CliExecutionPlan plan) {
        out.println("Mode: " + (plan.execute() ? "EXECUTE" : "DRY-RUN"));
        out.println("Config File: " + mask(plan.configPath() == null ? "" : plan.configPath().toString()));
        out.println("Database URL: " + mask(plan.databaseUrl()));
        out.println("Database User: " + maskOrDefault(plan.databaseUser(), "(not set)"));
        out.println("Output Directory: " + mask(plan.outputDirectory()));
        out.println("Output Formats: " + join(plan.outputFormats()));
        out.println("Run manifest enabled: " + plan.runManifestEnabled());
        out.println("Query Source: " + mask(plan.querySource()));
        if (plan.querySourceOverridden()) {
            out.println("Query Source Note: Multiple query sources configured; highest-priority source is used.");
        }
        out.println("Read-only query check: OK");
        out.println("Query Preview: " + mask(plan.queryPreview()));
    }

    public void renderDryRunSummary(PrintStream out, List<String> outputFormats) {
        out.println("Status: DRY_RUN");
        out.println("Planned output files: " + buildPlannedOutputs(outputFormats));
        out.println("Hint: Add --execute to run read-only query execution.");
    }

    public void renderExecuteSummary(PrintStream out, RunManifest manifest) {
        out.println("Status: " + mask(manifest.status()));
        out.println("Row Count: " + manifest.rowCount());
        out.println("Output Files: " + joinOutputFiles(manifest.outputFiles()));
        if (manifest.errorMessage() != null && !manifest.errorMessage().isBlank()) {
            out.println("Error Message: " + mask(manifest.errorMessage()));
        }
    }

    public void renderManifestPath(PrintStream out, Path manifestPath) {
        if (manifestPath == null) {
            return;
        }
        out.println("Manifest Path: " + mask(manifestPath.toString()));
    }

    private static String buildPlannedOutputs(List<String> outputFormats) {
        if (outputFormats == null || outputFormats.isEmpty()) {
            return "<run-id>.json";
        }
        StringBuilder planned = new StringBuilder();
        for (int i = 0; i < outputFormats.size(); i++) {
            if (i > 0) {
                planned.append(", ");
            }
            planned.append("<run-id>.").append(mask(outputFormats.get(i)));
        }
        return planned.toString();
    }

    private static String joinOutputFiles(List<OutputFileMetadata> outputFiles) {
        if (outputFiles == null || outputFiles.isEmpty()) {
            return "<none>";
        }
        StringBuilder joined = new StringBuilder();
        for (int i = 0; i < outputFiles.size(); i++) {
            if (i > 0) {
                joined.append(", ");
            }
            OutputFileMetadata file = outputFiles.get(i);
            joined.append(mask(file.path().isBlank() ? file.fileName() : file.path()));
        }
        return joined.toString();
    }

    private static String join(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "(none)";
        }
        StringBuilder joined = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                joined.append(",");
            }
            joined.append(mask(values.get(i)));
        }
        return joined.toString();
    }

    private static String maskOrDefault(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return mask(value);
    }

    private static String mask(String value) {
        return SecretMasker.maskSensitive(value == null ? "" : value);
    }
}
