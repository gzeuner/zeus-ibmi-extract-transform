package de.zeus.ibmi.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public final class ConfigLoader {

    private ConfigLoader() {
    }

    public static AppConfig load(Path file, Map<String, String> envOverrides) throws IOException {
        return load(file, envOverrides, Map.of());
    }

    public static AppConfig load(Path file, Map<String, String> envOverrides, Map<String, String> cliOverrides) throws IOException {
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            props.load(in);
        }
        return toAppConfig(props, new EnvironmentResolver(envOverrides), cliOverrides);
    }

    public static AppConfig loadExample(Map<String, String> envOverrides) throws IOException {
        Properties props = new Properties();
        try (InputStream in = ConfigLoader.class.getResourceAsStream("/config/example.application.properties")) {
            if (in == null) {
                throw new IOException("Example config not found at /config/example.application.properties");
            }
            props.load(in);
        }
        return toAppConfig(props, new EnvironmentResolver(envOverrides), Map.of());
    }

    static AppConfig toAppConfig(Properties props, EnvironmentResolver env, Map<String, String> cliOverrides) {
        String driver = pick(cliOverrides, "db.driver", env, "ZEUS_IBMI_DB_DRIVER", props.getProperty("db.driver"), null);
        String url = pick(cliOverrides, "db.url", env, "ZEUS_IBMI_DB_URL", props.getProperty("db.url"), null);
        String user = pick(cliOverrides, "db.user", env, "ZEUS_IBMI_DB_USER", props.getProperty("db.user"), null);
        String query = pick(cliOverrides, "query.sql", env, "ZEUS_IBMI_QUERY", props.getProperty("query.sql"), null);
        String queryFile = pick(cliOverrides, "query.file", env, "ZEUS_IBMI_QUERY_FILE", props.getProperty("query.file"), null);
        String formatsCsv = pick(cliOverrides, "output.formats", env, "ZEUS_IBMI_OUTPUT_FORMATS", props.getProperty("output.formats"), "xml,json,csv,md");
        String outputDirectory = pick(cliOverrides, "output.directory", env, "ZEUS_IBMI_OUTPUT_DIRECTORY", props.getProperty("output.directory"), null);

        String passwordEnvName = pick(cliOverrides, "db.passwordEnv", env, "ZEUS_IBMI_DB_PASSWORD_ENV", props.getProperty("db.passwordEnv"), null);
        String configuredPassword = pick(cliOverrides, "db.password", env, "ZEUS_IBMI_DB_PASSWORD", props.getProperty("db.password"), null);
        String passwordFromNamedEnv = passwordEnvName == null ? null : env.get(passwordEnvName);
        String password = firstNonBlank(configuredPassword, passwordFromNamedEnv);

        boolean runManifestEnabled = parseBoolean(
                pick(cliOverrides, "run.manifest.enabled", env, "ZEUS_IBMI_RUN_MANIFEST_ENABLED", props.getProperty("run.manifest.enabled"), "true"),
                "run.manifest.enabled");
        boolean allowEmptyPassword = parseBoolean(
                pick(cliOverrides, "db.allowEmptyPassword", env, "ZEUS_IBMI_DB_ALLOW_EMPTY_PASSWORD", props.getProperty("db.allowEmptyPassword"), "false"),
                "db.allowEmptyPassword");
        Integer fetchSize = parseInt(
                pick(cliOverrides, "query.fetchSize", env, "ZEUS_IBMI_QUERY_FETCH_SIZE", props.getProperty("query.fetchSize"), null),
                "query.fetchSize");
        Integer queryTimeoutSeconds = parseInt(
                pick(cliOverrides, "query.timeoutSeconds", env, "ZEUS_IBMI_QUERY_TIMEOUT_SECONDS", props.getProperty("query.timeoutSeconds"), null),
                "query.timeoutSeconds");

        List<OutputFormat> formats = OutputFormatParser.parseCsv(formatsCsv);

        AppConfig config = new AppConfig(
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
