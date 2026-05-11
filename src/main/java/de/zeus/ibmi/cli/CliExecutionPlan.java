package de.zeus.ibmi.cli;

import java.nio.file.Path;
import java.util.List;

public record CliExecutionPlan(
        Path configPath,
        boolean execute,
        String databaseUrl,
        String databaseUser,
        String outputDirectory,
        List<String> outputFormats,
        boolean runManifestEnabled,
        String queryPreview) {

    public CliExecutionPlan {
        outputFormats = outputFormats == null ? List.of() : List.copyOf(outputFormats);
        queryPreview = queryPreview == null ? "" : queryPreview;
    }
}
