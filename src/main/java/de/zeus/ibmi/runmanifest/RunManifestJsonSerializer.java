package de.zeus.ibmi.runmanifest;

import java.util.StringJoiner;

public final class RunManifestJsonSerializer {

    private RunManifestJsonSerializer() {
    }

    public static String toJson(RunManifest manifest) {
        StringJoiner outputs = toOutputFilesJsonArray(manifest.outputFiles());
        StringJoiner outputFormats = toJsonArray(manifest.outputFormats());
        return "{"
                + "\"toolName\":\"" + escape(manifest.toolName()) + "\"," 
                + "\"toolVersion\":\"" + escape(manifest.toolVersion()) + "\"," 
                + "\"runId\":\"" + escape(manifest.runId()) + "\"," 
                + "\"status\":\"" + escape(manifest.status()) + "\"," 
                + "\"dryRun\":" + manifest.dryRun() + ","
                + "\"startedAt\":\"" + toStringOrEmpty(manifest.startedAt()) + "\"," 
                + "\"finishedAt\":\"" + toStringOrEmpty(manifest.finishedAt()) + "\"," 
                + "\"durationMillis\":" + manifest.durationMillis() + ","
                + "\"configSource\":\"" + escape(manifest.configSource()) + "\"," 
                + "\"queryHash\":\"" + escape(manifest.queryHash()) + "\"," 
                + "\"queryPreview\":\"" + escape(manifest.queryPreview()) + "\"," 
                + "\"outputDirectory\":\"" + escape(manifest.outputDirectory()) + "\"," 
                + "\"outputFiles\":" + outputs + ","
                + "\"outputFormats\":" + outputFormats + ","
                + "\"rowCount\":" + manifest.rowCount() + ","
                + "\"columnCount\":" + manifest.columnCount() + ","
                + "\"errorClass\":\"" + escape(manifest.errorClass()) + "\"," 
                + "\"errorMessage\":\"" + escape(manifest.errorMessage()) + "\"," 
                + "\"javaVersion\":\"" + escape(manifest.javaVersion()) + "\"," 
                + "\"osName\":\"" + escape(manifest.osName()) + "\"," 
                + "\"osVersion\":\"" + escape(manifest.osVersion()) + "\""
                + "}";
    }

    private static StringJoiner toOutputFilesJsonArray(Iterable<OutputFileMetadata> files) {
        StringJoiner joiner = new StringJoiner(",", "[", "]");
        for (OutputFileMetadata file : files) {
            joiner.add("{"
                    + "\"format\":\"" + escape(file.format()) + "\"," 
                    + "\"path\":\"" + escape(file.path()) + "\"," 
                    + "\"fileName\":\"" + escape(file.fileName()) + "\"," 
                    + "\"sizeBytes\":" + file.sizeBytes() + ","
                    + "\"sha256\":\"" + escape(file.sha256()) + "\"," 
                    + "\"writtenAt\":\"" + toStringOrEmpty(file.writtenAt()) + "\""
                    + "}");
        }
        return joiner;
    }

    private static StringJoiner toJsonArray(Iterable<String> values) {
        StringJoiner joiner = new StringJoiner(",", "[", "]");
        for (String value : values) {
            joiner.add("\"" + escape(value) + "\"");
        }
        return joiner;
    }

    private static String toStringOrEmpty(Object value) {
        return value == null ? "" : escape(value.toString());
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
