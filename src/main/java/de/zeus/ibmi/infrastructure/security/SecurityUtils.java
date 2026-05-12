package de.zeus.ibmi.infrastructure.security;

public final class SecurityUtils {

  private SecurityUtils() {}

  public static String maskSecrets(String input) {
    return SecretMasker.maskSensitive(input);
  }
}
