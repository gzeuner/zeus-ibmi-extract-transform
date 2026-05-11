package de.zeus.ibmi.runmanifest;

import java.time.Instant;

public record OutputFileMetadata(
        String format,
        String path,
        String fileName,
        long sizeBytes,
        String sha256,
        Instant writtenAt) {

    public OutputFileMetadata {
        format = format == null ? "" : format;
        path = path == null ? "" : path;
        fileName = fileName == null ? "" : fileName;
        sizeBytes = Math.max(0L, sizeBytes);
        sha256 = sha256 == null ? "" : sha256;
    }
}
