package de.zeus.ibmi.runmanifest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class RunManifestWriter {

    public Path write(Path outputDirectory, RunManifest manifest) {
        try {
            Files.createDirectories(outputDirectory);
            Path file = outputDirectory.resolve(manifest.runId() + ".manifest.json");
            Files.writeString(file, RunManifestJsonSerializer.toJson(manifest), StandardCharsets.UTF_8);
            return file;
        } catch (IOException ex) {
            throw new ManifestException("Failed to write run manifest.", ex);
        }
    }
}
