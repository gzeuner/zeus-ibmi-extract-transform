package de.zeus.ibmi.runmanifest;

import de.zeus.ibmi.security.SecretMasker;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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
            String querySourceType,
            String querySource,
            String queryHash,
            String queryPreview,
            Path outputDirectory,
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
                querySourceType,
                ManifestPathFormatter.querySourceDisplay(querySource),
                queryHash,
                queryPreview,
                ManifestPathFormatter.outputDirectoryDisplay(outputDirectory),
                toOutputFileMetadata(outputDirectory, outputFiles),
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
            String querySourceType,
            String querySource,
            String queryHash,
            String queryPreview,
            Path outputDirectory,
            List<Path> outputFiles,
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
                querySourceType,
                ManifestPathFormatter.querySourceDisplay(querySource),
                queryHash,
                queryPreview,
                ManifestPathFormatter.outputDirectoryDisplay(outputDirectory),
                toOutputFileMetadata(outputDirectory, outputFiles),
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
            String querySourceType,
            String querySource,
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
                querySourceType,
                ManifestPathFormatter.querySourceDisplay(querySource),
                queryHash,
                queryPreview,
                ManifestPathFormatter.outputDirectoryDisplay(outputDirectory),
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

    private static List<OutputFileMetadata> toOutputFileMetadata(Path outputDirectory, List<Path> outputFiles) {
        if (outputFiles == null || outputFiles.isEmpty()) {
            return List.of();
        }
        Path base = outputDirectory == null ? null : outputDirectory.toAbsolutePath().normalize();
        List<OutputFileMetadata> metadata = new ArrayList<>(outputFiles.size());
        for (Path outputFile : outputFiles) {
            Path absoluteFile = outputFile == null ? null : outputFile.toAbsolutePath().normalize();
            if (absoluteFile == null) {
                continue;
            }
            String relativePath = ManifestPathFormatter.outputFilePathDisplay(base, absoluteFile);
            String fileName = absoluteFile.getFileName() == null ? "" : absoluteFile.getFileName().toString();
            String format = formatFromFileName(fileName);
            long sizeBytes = fileSize(absoluteFile);
            String sha256 = OutputChecksumCalculator.sha256(absoluteFile);
            Instant writtenAt = lastModifiedTime(absoluteFile);
            metadata.add(new OutputFileMetadata(format, relativePath, fileName, sizeBytes, sha256, writtenAt));
        }
        return List.copyOf(metadata);
    }

    private static long fileSize(Path file) {
        try {
            return Files.size(file);
        } catch (IOException ex) {
            throw new ManifestException("Failed to read output size for file: " + file, ex);
        }
    }

    private static Instant lastModifiedTime(Path file) {
        try {
            return Files.getLastModifiedTime(file).toInstant();
        } catch (IOException ex) {
            return null;
        }
    }

    private static String formatFromFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "";
        }
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1);
    }

    private static long durationMillis(Instant startedAt, Instant finishedAt) {
        if (startedAt == null || finishedAt == null) {
            return 0L;
        }
        return Duration.between(startedAt, finishedAt).toMillis();
    }
}
