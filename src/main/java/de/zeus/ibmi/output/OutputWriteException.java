package de.zeus.ibmi.output;

public class OutputWriteException extends RuntimeException {
    public OutputWriteException(String message) {
        super(message);
    }

    public OutputWriteException(String message, Throwable cause) {
        super(message, cause);
    }
}
