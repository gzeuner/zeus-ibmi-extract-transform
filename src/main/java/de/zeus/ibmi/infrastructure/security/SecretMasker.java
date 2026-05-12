package de.zeus.ibmi.infrastructure.security;

import java.util.regex.Pattern;

public final class SecretMasker {

  private static final Pattern KEY_VALUE_PATTERN =
      Pattern.compile(
          "(?i)\\b(password|pwd|token|secret|api[_-]?key|access[_-]?token|refresh[_-]?token)\\b\\s*=\\s*([^\\s,;]+)");
  private static final Pattern JSON_VALUE_PATTERN =
      Pattern.compile(
          "(?i)(\"(?:password|pwd|token|secret|api[_-]?key|access[_-]?token|refresh[_-]?token)\"\\s*:\\s*\")([^\"]*)(\")");
  private static final Pattern JDBC_USERINFO_PATTERN =
      Pattern.compile("(?i)(jdbc:[a-z0-9]+://)([^:@/\\s]+):([^@/\\s]+)@");

  private SecretMasker() {}

  public static String maskSensitive(String text) {
    if (text == null) {
      return null;
    }

    String masked = KEY_VALUE_PATTERN.matcher(text).replaceAll("$1=***");
    masked = JSON_VALUE_PATTERN.matcher(masked).replaceAll("$1***$3");
    masked = JDBC_USERINFO_PATTERN.matcher(masked).replaceAll("$1$2:***@");
    return masked;
  }
}
