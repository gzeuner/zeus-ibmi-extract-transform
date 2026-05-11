package de.zeus.ibmi.runmanifest;

import java.time.Instant;
import java.util.List;

public record RunManifest(
        String toolName,
        String toolVersion,
        String runId,
        String status,
        boolean dryRun,
        Instant startedAt,
        Instant finishedAt,
        long durationMillis,
        String configSource,
        String queryHash,
        String queryPreview,
        String outputDirectory,
        List<OutputFileMetadata> outputFiles,
        List<String> outputFormats,
        int rowCount,
        int columnCount,
        String errorClass,
        String errorMessage,
        String javaVersion,
        String osName,
        String osVersion) {

    public RunManifest {
        outputFiles = outputFiles == null ? List.of() : List.copyOf(outputFiles);
        outputFormats = outputFormats == null ? List.of() : List.copyOf(outputFormats);
    }
}
