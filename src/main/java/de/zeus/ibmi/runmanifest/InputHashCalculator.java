package de.zeus.ibmi.runmanifest;

import de.zeus.ibmi.infrastructure.config.AppConfig;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.StringJoiner;

public final class InputHashCalculator {

  private InputHashCalculator() {}

  public static String sha256For(AppConfig config, String normalizedQuery) {
    StringJoiner joiner = new StringJoiner("|");
    joiner.add(nullSafe(config.databaseDriver()));
    joiner.add(nullSafe(config.databaseUrl()));
    joiner.add(nullSafe(config.username()));
    joiner.add(nullSafe(normalizedQuery));
    joiner.add(String.join(",", config.outputFormatIds()));
    joiner.add(nullSafe(config.outputDirectory()));
    joiner.add(nullSafe(config.htmlTheme()));
    joiner.add(nullSafe(config.htmlCustomCssFile()));
    joiner.add(String.valueOf(config.htmlIncludeManifest()));
    return "sha256:" + hex(sha256(joiner.toString()));
  }

  private static byte[] sha256(String input) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return digest.digest(input.getBytes(StandardCharsets.UTF_8));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }

  private static String hex(byte[] bytes) {
    StringBuilder out = new StringBuilder(bytes.length * 2);
    for (byte b : bytes) {
      out.append(String.format("%02x", b));
    }
    return out.toString();
  }

  private static String nullSafe(String value) {
    return value == null ? "" : value;
  }
}
