package de.zeus.ibmi.infrastructure.cli;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;

public record CliArguments(
    boolean help,
    boolean version,
    boolean execute,
    Path configPath,
    Map<String, String> configOverrides) {

  public static CliArguments parse(String[] args) {
    PicocliOptions options = new PicocliOptions();
    CommandLine commandLine = new CommandLine(options);
    commandLine.setCaseInsensitiveEnumValuesAllowed(true);
    commandLine.setUnmatchedArgumentsAllowed(false);
    commandLine.setOverwrittenOptionsAllowed(true);
    try {
      commandLine.parseArgs(args);
    } catch (ParameterException ex) {
      throw new CliArgumentException(legacyMessage(ex.getMessage()), ex);
    }

    Map<String, String> overrides = new HashMap<>();
    putIfNonBlank(overrides, "db.driver", options.dbDriver);
    putIfNonBlank(overrides, "db.url", options.dbUrl);
    putIfNonBlank(overrides, "db.user", options.dbUser);
    putIfNonBlank(overrides, "db.password", options.dbPassword);
    putIfNonBlank(overrides, "db.passwordEnv", options.dbPasswordEnv);
    putIfNonBlank(overrides, "query.sql", options.query);
    putIfNonBlank(overrides, "query.file", options.queryFile);
    putIfNonBlank(overrides, "output.directory", options.outputDirectory);
    putIfNonBlank(overrides, "output.formats", options.outputFormats);
    putIfNonBlank(overrides, "query.fetchSize", options.fetchSize);
    putIfNonBlank(overrides, "query.timeoutSeconds", options.queryTimeoutSeconds);
    if (options.manifestEnabled) {
      overrides.put("run.manifest.enabled", "true");
    }
    if (options.manifestDisabled) {
      overrides.put("run.manifest.enabled", "false");
    }
    if (options.allowEmptyPassword) {
      overrides.put("db.allowEmptyPassword", "true");
    }

    Path configPath = options.configPath;
    boolean execute = options.execute && !options.dryRun;
    return new CliArguments(
        options.help, options.version, execute, configPath, Map.copyOf(overrides));
  }

  private static String legacyMessage(String message) {
    if (message == null) {
      return "Invalid arguments. Use --help for usage.";
    }
    if (message.startsWith("Unknown option:")) {
      String option = message.replace("Unknown option:", "").trim().replace("'", "");
      return "Unknown argument: " + option + ". Use --help for usage.";
    }
    if (message.contains("option '--config'") && message.contains("Missing required parameter")) {
      return "Missing value for --config";
    }
    if (message.contains("option '--config'") && message.contains("Expected parameter")) {
      return "Missing value for --config";
    }
    if (message.contains("option '--query-file'")
        && message.contains("Missing required parameter")) {
      return "Missing value for --query-file";
    }
    if (message.contains("option '--query-file'") && message.contains("Expected parameter")) {
      return "Missing value for --query-file";
    }
    return message;
  }

  private static void putIfNonBlank(Map<String, String> map, String key, String value) {
    if (value != null && !value.isBlank()) {
      map.put(key, value.trim());
    }
  }

  @Command(name = "zeus-ibmi-extract-transform", sortOptions = false)
  static final class PicocliOptions {
    @Option(
        names = {"-h", "--help"},
        usageHelp = true,
        description = "Show help")
    boolean help;

    @Option(
        names = {"-v", "--version"},
        versionHelp = true,
        description = "Show version")
    boolean version;

    @Option(
        names = {"-x", "--execute"},
        description = "Execute read-only query (default is dry-run)")
    boolean execute;

    @Option(names = "--dry-run", description = "Force dry-run mode")
    boolean dryRun;

    @Option(names = "--config", description = "Config file path")
    Path configPath;

    @Option(names = "--db-driver", description = "Override JDBC driver")
    String dbDriver;

    @Option(names = "--db-url", description = "Override JDBC URL")
    String dbUrl;

    @Option(names = "--db-user", description = "Override JDBC user")
    String dbUser;

    @Option(names = "--db-password", description = "Override JDBC password")
    String dbPassword;

    @Option(
        names = "--db-password-env",
        description = "Resolve password from named environment variable")
    String dbPasswordEnv;

    @Option(names = "--query", description = "Override SQL query")
    String query;

    @Option(names = "--query-file", description = "Override SQL query file (UTF-8)")
    String queryFile;

    @Option(names = "--output-dir", description = "Override output directory")
    String outputDirectory;

    @Option(
        names = "--output-formats",
        description = "Override output formats (xml,json,jsonl,csv,md,html)")
    String outputFormats;

    @Option(names = "--manifest-enabled", description = "Force run manifest enabled")
    boolean manifestEnabled;

    @Option(names = "--manifest-disabled", description = "Force run manifest disabled")
    boolean manifestDisabled;

    @Option(names = "--fetch-size", description = "Override JDBC fetch size")
    String fetchSize;

    @Option(
        names = "--query-timeout-seconds",
        description = "Override JDBC query timeout in seconds")
    String queryTimeoutSeconds;

    @Option(
        names = "--allow-empty-password",
        description = "Allow empty db.password for local scenarios")
    boolean allowEmptyPassword;
  }
}
