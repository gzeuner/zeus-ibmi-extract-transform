package de.zeus.ibmi.infrastructure.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.FileSystemResource;

public final class ConfigLoader {

  private ConfigLoader() {}

  public static AppConfig load(Path file, Map<String, String> envOverrides) throws IOException {
    return load(file, envOverrides, Map.of());
  }

  public static AppConfig load(
      Path file, Map<String, String> envOverrides, Map<String, String> cliOverrides)
      throws IOException {
    Properties props = loadConfigFile(file);
    return toAppConfig(props, new EnvironmentResolver(envOverrides), cliOverrides);
  }

  public static AppConfig loadExample(Map<String, String> envOverrides) throws IOException {
    Properties props = new Properties();
    try (InputStream in =
        ConfigLoader.class.getResourceAsStream("/config/example.application.properties")) {
      if (in == null) {
        throw new IOException("Example config not found at /config/example.application.properties");
      }
      props.load(in);
    }
    return toAppConfig(props, new EnvironmentResolver(envOverrides), Map.of());
  }

  static AppConfig toAppConfig(
      Properties props, EnvironmentResolver env, Map<String, String> cliOverrides) {
    String driver =
        pick(
            cliOverrides,
            "db.driver",
            env,
            "ZEUS_IBMI_DB_DRIVER",
            props.getProperty("db.driver"),
            null);
    String url =
        pick(cliOverrides, "db.url", env, "ZEUS_IBMI_DB_URL", props.getProperty("db.url"), null);
    String user =
        pick(cliOverrides, "db.user", env, "ZEUS_IBMI_DB_USER", props.getProperty("db.user"), null);
    String query =
        pick(
            cliOverrides,
            "query.sql",
            env,
            "ZEUS_IBMI_QUERY",
            props.getProperty("query.sql"),
            null);
    String queryFile =
        pick(
            cliOverrides,
            "query.file",
            env,
            "ZEUS_IBMI_QUERY_FILE",
            props.getProperty("query.file"),
            null);
    String formatsCsv =
        pick(
            cliOverrides,
            "output.formats",
            env,
            "ZEUS_IBMI_OUTPUT_FORMATS",
            props.getProperty("output.formats"),
            "xml,json,csv,md");
    String outputDirectory =
        pick(
            cliOverrides,
            "output.directory",
            env,
            "ZEUS_IBMI_OUTPUT_DIRECTORY",
            props.getProperty("output.directory"),
            null);

    String passwordEnvName =
        pick(
            cliOverrides,
            "db.passwordEnv",
            env,
            "ZEUS_IBMI_DB_PASSWORD_ENV",
            props.getProperty("db.passwordEnv"),
            null);
    String configuredPassword =
        pick(
            cliOverrides,
            "db.password",
            env,
            "ZEUS_IBMI_DB_PASSWORD",
            props.getProperty("db.password"),
            null);
    String passwordFromNamedEnv = passwordEnvName == null ? null : env.get(passwordEnvName);
    String password = firstNonBlank(configuredPassword, passwordFromNamedEnv);

    boolean runManifestEnabled =
        parseBoolean(
            pick(
                cliOverrides,
                "run.manifest.enabled",
                env,
                "ZEUS_IBMI_RUN_MANIFEST_ENABLED",
                props.getProperty("run.manifest.enabled"),
                "true"),
            "run.manifest.enabled");
    boolean allowEmptyPassword =
        parseBoolean(
            pick(
                cliOverrides,
                "db.allowEmptyPassword",
                env,
                "ZEUS_IBMI_DB_ALLOW_EMPTY_PASSWORD",
                props.getProperty("db.allowEmptyPassword"),
                "false"),
            "db.allowEmptyPassword");
    Integer fetchSize =
        parseInt(
            pick(
                cliOverrides,
                "query.fetchSize",
                env,
                "ZEUS_IBMI_QUERY_FETCH_SIZE",
                props.getProperty("query.fetchSize"),
                null),
            "query.fetchSize");
    Integer queryTimeoutSeconds =
        parseInt(
            pick(
                cliOverrides,
                "query.timeoutSeconds",
                env,
                "ZEUS_IBMI_QUERY_TIMEOUT_SECONDS",
                props.getProperty("query.timeoutSeconds"),
                null),
            "query.timeoutSeconds");

    List<OutputFormat> formats = OutputFormatParser.parseCsv(formatsCsv);

    AppConfig config =
        new AppConfig(
            driver,
            url,
            user,
            password,
            passwordEnvName,
            query,
            queryFile,
            outputDirectory,
            formats,
            runManifestEnabled,
            fetchSize,
            queryTimeoutSeconds,
            allowEmptyPassword);
    config.validateAndPrepareOutputDirectory();
    ensureOutputDirectoryCreatable(config.outputDirectory());
    return config;
  }

  private static Properties loadConfigFile(Path file) throws IOException {
    String fileName = file.getFileName() == null ? "" : file.getFileName().toString().toLowerCase();
    if (fileName.endsWith(".yml") || fileName.endsWith(".yaml")) {
      return loadYaml(file);
    }
    Properties props = new Properties();
    try (InputStream in = Files.newInputStream(file)) {
      props.load(in);
    }
    return props;
  }

  private static Properties loadYaml(Path file) {
    YamlPropertiesFactoryBean yamlFactory = new YamlPropertiesFactoryBean();
    yamlFactory.setResources(new FileSystemResource(file));
    Properties raw = yamlFactory.getObject();
    if (raw == null) {
      throw new ConfigValidationException("Unable to read YAML config: " + file);
    }
    return normalizeYamlProperties(raw);
  }

  private static Properties normalizeYamlProperties(Properties yamlProps) {
    Map<String, String> normalized = new LinkedHashMap<>();
    for (Map.Entry<Object, Object> entry : yamlProps.entrySet()) {
      String key = String.valueOf(entry.getKey());
      String value = entry.getValue() == null ? null : String.valueOf(entry.getValue());
      if (key.startsWith("zeus.ibmi.")) {
        String legacyKey =
            switch (key) {
              case "zeus.ibmi.db.driver" -> "db.driver";
              case "zeus.ibmi.db.url" -> "db.url";
              case "zeus.ibmi.db.user" -> "db.user";
              case "zeus.ibmi.db.password" -> "db.password";
              case "zeus.ibmi.db.password-env", "zeus.ibmi.db.passwordEnv" -> "db.passwordEnv";
              case "zeus.ibmi.db.allow-empty-password", "zeus.ibmi.db.allowEmptyPassword" ->
                  "db.allowEmptyPassword";
              case "zeus.ibmi.query.sql" -> "query.sql";
              case "zeus.ibmi.query.file" -> "query.file";
              case "zeus.ibmi.query.fetch-size", "zeus.ibmi.query.fetchSize" -> "query.fetchSize";
              case "zeus.ibmi.query.timeout-seconds", "zeus.ibmi.query.timeoutSeconds" ->
                  "query.timeoutSeconds";
              case "zeus.ibmi.output.directory" -> "output.directory";
              case "zeus.ibmi.manifest.enabled" -> "run.manifest.enabled";
              default -> null;
            };
        if (legacyKey != null) {
          normalized.put(legacyKey, value);
        }
        continue;
      }
      normalized.put(key, value);
    }

    String formats = flattenFormats(yamlProps);
    if (isNonBlank(formats)) {
      normalized.put("output.formats", formats);
    } else if (yamlProps.containsKey("zeus.ibmi.output.formats")) {
      normalized.put("output.formats", String.valueOf(yamlProps.get("zeus.ibmi.output.formats")));
    }

    Properties properties = new Properties();
    normalized.forEach(properties::setProperty);
    return properties;
  }

  private static String flattenFormats(Properties yamlProps) {
    List<String> values = new ArrayList<>();
    for (int i = 0; ; i++) {
      String value = yamlProps.getProperty("zeus.ibmi.output.formats[" + i + "]");
      if (value == null) {
        break;
      }
      values.add(value.trim());
    }
    if (values.isEmpty()) {
      return null;
    }
    return String.join(",", values);
  }

  private static String pick(
      Map<String, String> cliOverrides,
      String cliKey,
      EnvironmentResolver env,
      String envKey,
      String fileValue,
      String defaultValue) {
    String cliValue = cliOverrides == null ? null : cliOverrides.get(cliKey);
    if (isNonBlank(cliValue)) {
      return cliValue;
    }
    String envValue = env == null ? null : env.get(envKey);
    if (isNonBlank(envValue)) {
      return envValue;
    }
    if (isNonBlank(fileValue)) {
      return fileValue;
    }
    return defaultValue;
  }

  private static boolean parseBoolean(String value, String propertyName) {
    if (value == null) {
      return false;
    }
    String normalized = value.trim().toLowerCase();
    if ("true".equals(normalized)) {
      return true;
    }
    if ("false".equals(normalized)) {
      return false;
    }
    throw new ConfigValidationException("Invalid boolean for " + propertyName + ": " + value);
  }

  private static Integer parseInt(String value, String propertyName) {
    if (!isNonBlank(value)) {
      return null;
    }
    try {
      return Integer.valueOf(value.trim());
    } catch (NumberFormatException ex) {
      throw new ConfigValidationException("Invalid integer for " + propertyName + ": " + value, ex);
    }
  }

  private static String firstNonBlank(String... values) {
    for (String value : values) {
      if (isNonBlank(value)) {
        return value;
      }
    }
    return null;
  }

  private static boolean isNonBlank(String value) {
    return value != null && !value.trim().isEmpty();
  }

  private static void ensureOutputDirectoryCreatable(String outputDirectory) {
    if (!isNonBlank(outputDirectory)) {
      throw new ConfigValidationException("output.directory is required");
    }
    Path outputPath = Path.of(outputDirectory);
    try {
      Files.createDirectories(outputPath);
    } catch (IOException ex) {
      throw new ConfigValidationException("Cannot create output directory: " + outputDirectory, ex);
    }
  }
}
