package de.zeus.ibmi.query;

import de.zeus.ibmi.config.ConfigValidationException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class QueryFileLoader {

    public String load(Path path) {
        if (path == null) {
            throw new ConfigValidationException("Query file path is required.");
        }
        if (!Files.exists(path)) {
            throw new ConfigValidationException("Query file does not exist: " + path);
        }
        if (Files.isDirectory(path)) {
            throw new ConfigValidationException("Query file path points to a directory: " + path);
        }
        if (!Files.isReadable(path)) {
            throw new ConfigValidationException("Query file is not readable: " + path);
        }

        final String content;
        try {
            content = Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new ConfigValidationException("Unable to read query file: " + path, ex);
        }

        if (content.trim().isEmpty()) {
            throw new ConfigValidationException("Query file is empty: " + path);
        }
        if (containsOnlyWhitespaceAndComments(content)) {
            throw new ConfigValidationException("Query file contains only comments or whitespace: " + path);
        }
        return content;
    }

    private static boolean containsOnlyWhitespaceAndComments(String content) {
        String withoutBlockComments = content.replaceAll("(?s)/\\*.*?\\*/", " ");
        String withoutLineComments = withoutBlockComments.replaceAll("(?m)--.*$", " ");
        return withoutLineComments.trim().isEmpty();
    }
}
