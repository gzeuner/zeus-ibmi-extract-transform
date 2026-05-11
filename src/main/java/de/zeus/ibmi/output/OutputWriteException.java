package de.zeus.ibmi.output;

import java.nio.file.Path;
import java.util.List;

public class OutputWriteException extends RuntimeException {
    private final List<Path> writtenFiles;

    public OutputWriteException(String message) {
        this(message, null, List.of());
    }

    public OutputWriteException(String message, Throwable cause) {
        this(message, cause, List.of());
    }

    public OutputWriteException(String message, List<Path> writtenFiles) {
        this(message, null, writtenFiles);
    }

    public OutputWriteException(String message, Throwable cause, List<Path> writtenFiles) {
        super(message, cause);
        this.writtenFiles = writtenFiles == null ? List.of() : List.copyOf(writtenFiles);
    }

    public List<Path> writtenFiles() {
        return writtenFiles;
    }
}
