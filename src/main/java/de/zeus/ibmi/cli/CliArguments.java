package de.zeus.ibmi.cli;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public record CliArguments(
        boolean help,
        boolean version,
        boolean execute,
        Path configPath,
        Map<String, String> configOverrides) {

    public static CliArguments parse(String[] args) {
        boolean help = false;
        boolean version = false;
        boolean execute = false;
        Path configPath = null;
        Map<String, String> overrides = new HashMap<>();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "--help":
                case "-h":
                    help = true;
                    break;
                case "--version":
                case "-v":
                    version = true;
                    break;
                case "--execute":
                case "-x":
                    execute = true;
                    break;
                case "--config":
                    configPath = Path.of(readValue(args, ++i, "--config"));
                    break;
                case "--db-driver":
                    overrides.put("db.driver", readValue(args, ++i, "--db-driver"));
                    break;
                case "--db-url":
                    overrides.put("db.url", readValue(args, ++i, "--db-url"));
                    break;
                case "--db-user":
                    overrides.put("db.user", readValue(args, ++i, "--db-user"));
                    break;
                case "--db-password":
                    overrides.put("db.password", readValue(args, ++i, "--db-password"));
                    break;
                case "--db-password-env":
                    overrides.put("db.passwordEnv", readValue(args, ++i, "--db-password-env"));
                    break;
                case "--query":
                    overrides.put("query.sql", readValue(args, ++i, "--query"));
                    break;
                case "--output-dir":
                    overrides.put("output.directory", readValue(args, ++i, "--output-dir"));
                    break;
                case "--output-formats":
                    overrides.put("output.formats", readValue(args, ++i, "--output-formats"));
                    break;
                case "--manifest-enabled":
                    overrides.put("run.manifest.enabled", "true");
                    break;
                case "--manifest-disabled":
                    overrides.put("run.manifest.enabled", "false");
                    break;
                case "--fetch-size":
                    overrides.put("query.fetchSize", readValue(args, ++i, "--fetch-size"));
                    break;
                case "--query-timeout-seconds":
                    overrides.put("query.timeoutSeconds", readValue(args, ++i, "--query-timeout-seconds"));
                    break;
                case "--allow-empty-password":
                    overrides.put("db.allowEmptyPassword", "true");
                    break;
                default:
                    throw new CliArgumentException("Unknown argument: " + arg + ". Use --help for usage.");
            }
        }
        return new CliArguments(help, version, execute, configPath, Map.copyOf(overrides));
    }

    private static String readValue(String[] args, int idx, String option) {
        if (idx >= args.length) {
            throw new CliArgumentException("Missing value for " + option);
        }
        String value = args[idx];
        if (value.startsWith("-")) {
            throw new CliArgumentException("Missing value for " + option);
        }
        return value;
    }
}
