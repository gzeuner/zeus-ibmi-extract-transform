package de.zeus.ibmi.infrastructure.config;

import java.util.List;
import java.util.stream.Collectors;

public record AppConfig(
    String databaseDriver,
    String databaseUrl,
    String username,
    String password,
    String passwordEnvName,
    String query,
    String queryFile,
    String outputDirectory,
    List<OutputFormat> outputFormats,
    boolean runManifestEnabled,
    Integer fetchSize,
    Integer queryTimeoutSeconds,
    boolean allowEmptyPassword) {

  public AppConfig {
    databaseDriver = trimToNull(databaseDriver);
    databaseUrl = trimToNull(databaseUrl);
    username = trimToNull(username);
    password = password == null ? null : password;
    passwordEnvName = trimToNull(passwordEnvName);
    query = trimToNull(query);
    queryFile = trimToNull(queryFile);
    outputDirectory = trimToNull(outputDirectory);
    if (outputFormats == null || outputFormats.isEmpty()) {
      outputFormats =
          List.of(
              OutputFormat.XML,
              OutputFormat.JSON,
              OutputFormat.CSV,
              OutputFormat.MD,
              OutputFormat.HTML);
    } else {
      outputFormats = List.copyOf(outputFormats);
    }
    if (fetchSize != null && fetchSize <= 0) {
      throw new ConfigValidationException("fetchSize must be greater than 0.");
    }
    if (queryTimeoutSeconds != null && queryTimeoutSeconds <= 0) {
      throw new ConfigValidationException("queryTimeoutSeconds must be greater than 0.");
    }
  }

  private static String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  public List<String> outputFormatIds() {
    return outputFormats.stream().map(OutputFormat::id).collect(Collectors.toList());
  }

  public void validateAndPrepareOutputDirectory() {
    if (databaseUrl == null) {
      throw new ConfigValidationException("db.url is required");
    }
    if (query == null && queryFile == null) {
      throw new ConfigValidationException("query.sql or query.file is required");
    }
    if (outputDirectory == null) {
      throw new ConfigValidationException("output.directory is required");
    }
    if ((password == null || password.isEmpty()) && !allowEmptyPassword && !isH2Url(databaseUrl)) {
      throw new ConfigValidationException(
          "db.password is required unless allowEmptyPassword=true or H2 is used.");
    }
  }

  public AppConfig withQuery(String resolvedQuery) {
    return new AppConfig(
        databaseDriver,
        databaseUrl,
        username,
        password,
        passwordEnvName,
        resolvedQuery,
        queryFile,
        outputDirectory,
        outputFormats,
        runManifestEnabled,
        fetchSize,
        queryTimeoutSeconds,
        allowEmptyPassword);
  }

  private static boolean isH2Url(String url) {
    return url != null && url.startsWith("jdbc:h2:");
  }
}
