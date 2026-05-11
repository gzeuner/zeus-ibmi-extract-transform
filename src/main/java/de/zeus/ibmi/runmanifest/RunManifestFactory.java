package de.zeus.ibmi.runmanifest;

import de.zeus.ibmi.security.SecretMasker;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public final class RunManifestFactory {

    private RunManifestFactory() {
    }

    public static RunManifest success(
            String toolName,
            String toolVersion,
            String runId,
            Instant startedAt,
            Instant finishedAt,
            String configSource,
            String queryHash,
            String queryPreview,
            String outputDirectory,
            List<Path> outputFiles,
            List<String> outputFormats,
            int rowCount,
            int columnCount) {
        return new RunManifest(
                toolName,
                toolVersion,
                runId,
                "SUCCESS",
                false,
                startedAt,
                finishedAt,
                durationMillis(startedAt, finishedAt),
                configSource,
                queryHash,
                queryPreview,
                outputDirectory,
                outputFiles.stream().map(Path::toString).collect(Collectors.toList()),
                outputFormats,
                rowCount,
                columnCount,
                "",
                "",
                System.getProperty("java.version", ""),
                System.getProperty("os.name", ""),
                System.getProperty("os.version", ""));
    }

    public static RunManifest failure(
            String toolName,
            String toolVersion,
            String runId,
            Instant startedAt,
            Instant finishedAt,
            String configSource,
            String queryHash,
            String queryPreview,
            String outputDirectory,
            List<String> outputFormats,
            Throwable error,
            String sanitizedErrorMessage) {
        return new RunManifest(
                toolName,
                toolVersion,
                runId,
                "FAILED",
                false,
                startedAt,
                finishedAt,
                durationMillis(startedAt, finishedAt),
                configSource,
                queryHash,
                queryPreview,
                outputDirectory,
                List.of(),
                outputFormats,
                0,
                0,
                error == null ? "" : error.getClass().getName(),
                sanitizedErrorMessage == null ? "" : SecretMasker.maskSensitive(sanitizedErrorMessage),
                System.getProperty("java.version", ""),
                System.getProperty("os.name", ""),
                System.getProperty("os.version", ""));
    }

    public static RunManifest dryRun(
            String toolName,
            String toolVersion,
            String runId,
            Instant startedAt,
            Instant finishedAt,
            String configSource,
            String queryHash,
            String queryPreview,
            String outputDirectory,
            List<String> outputFormats) {
        return new RunManifest(
                toolName,
                toolVersion,
                runId,
                "DRY_RUN",
                true,
                startedAt,
                finishedAt,
                durationMillis(startedAt, finishedAt),
                configSource,
                queryHash,
                queryPreview,
                outputDirectory,
                List.of(),
                outputFormats,
                0,
                0,
                "",
                "",
                System.getProperty("java.version", ""),
                System.getProperty("os.name", ""),
                System.getProperty("os.version", ""));
    }

    private static long durationMillis(Instant startedAt, Instant finishedAt) {
        if (startedAt == null || finishedAt == null) {
            return 0L;
        }
        return Duration.between(startedAt, finishedAt).toMillis();
    }
}
