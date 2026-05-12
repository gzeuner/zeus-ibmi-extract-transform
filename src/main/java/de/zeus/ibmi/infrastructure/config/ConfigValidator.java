package de.zeus.ibmi.infrastructure.config;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Set;

public final class ConfigValidator {

  private final Validator validator;

  public ConfigValidator(Validator validator) {
    this.validator = validator;
  }

  public void validate(ZeusIbmiProperties properties) {
    Set<ConstraintViolation<ZeusIbmiProperties>> violations = validator.validate(properties);
    if (!violations.isEmpty()) {
      ConstraintViolation<ZeusIbmiProperties> violation = violations.iterator().next();
      throw new ConfigValidationException(violation.getMessage());
    }
    String sql = trimToNull(properties.getQuery().getSql());
    String file = trimToNull(properties.getQuery().getFile());
    if (sql == null && file == null) {
      throw new ConfigValidationException("query.sql or query.file is required");
    }
  }

  private static String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }
}
