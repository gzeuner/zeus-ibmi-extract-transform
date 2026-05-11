package de.zeus.ibmi.cli;

import de.zeus.ibmi.config.ConfigValidationException;

public final class CliArgumentException extends ConfigValidationException {

    public CliArgumentException(String message) {
        super(message);
    }
}
