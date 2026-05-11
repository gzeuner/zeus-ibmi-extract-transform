package de.zeus.ibmi.runmanifest;

import java.nio.file.Path;

final class ManifestPathFormatter {

    static final String REDACTED_OUTPUT_DIRECTORY = "<output-directory>";
    static final String REDACTED_OUTPUT_FILE = "<output-file>";

    private ManifestPathFormatter() {
    }

    static String outputDirectoryDisplay(Path outputDirectory) {
        if (outputDirectory == null) {
            return "";
        }
        if (outputDirectory.isAbsolute()) {
            return REDACTED_OUTPUT_DIRECTORY;
        }
        return normalize(outputDirectory.toString());
    }

    static String outputDirectoryDisplay(String configuredOutputDirectory) {
        if (configuredOutputDirectory == null) {
            return "";
        }
        String normalized = normalize(configuredOutputDirectory.trim());
        if (normalized.isEmpty()) {
            return "";
        }
        return looksAbsolute(normalized) ? REDACTED_OUTPUT_DIRECTORY : normalized;
    }

    static String outputFilePathDisplay(Path outputDirectory, Path outputFile) {
        if (outputFile == null) {
            return "";
        }
        Path absoluteFile = outputFile.toAbsolutePath().normalize();
        Path absoluteOutputDirectory = outputDirectory == null ? null : outputDirectory.toAbsolutePath().normalize();
        if (absoluteOutputDirectory != null) {
            try {
                String relative = normalize(absoluteOutputDirectory.relativize(absoluteFile).toString());
                if (isSafeRelative(relative)) {
                    return relative;
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
        Path fileName = absoluteFile.getFileName();
        if (fileName != null) {
            String safeName = normalize(fileName.toString());
            if (!safeName.isBlank()) {
                return safeName;
            }
        }
        String fallback = normalize(absoluteFile.toString());
        return looksAbsolute(fallback) ? REDACTED_OUTPUT_FILE : fallback;
    }

    private static boolean isSafeRelative(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        if (looksAbsolute(value)) {
            return false;
        }
        return !value.startsWith("../") && !value.equals("..");
    }

    private static boolean looksAbsolute(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        return value.startsWith("/")
                || value.startsWith("//")
                || value.matches("^[A-Za-z]:[\\\\/].*");
    }

    private static String normalize(String value) {
        return value == null ? "" : value.replace('\\', '/');
    }
}
