package de.zeus.ibmi.infrastructure.cli;

import de.zeus.ibmi.infrastructure.config.ConfigValidationException;

public final class CliArgumentException extends ConfigValidationException {

  public CliArgumentException(String message) {
    super(message);
  }

  public CliArgumentException(String message, Throwable cause) {
    super(message, cause);
  }
}
