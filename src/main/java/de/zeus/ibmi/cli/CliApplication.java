package de.zeus.ibmi.cli;

import de.zeus.ibmi.config.AppConfig;
import de.zeus.ibmi.config.ConfigValidationException;
import de.zeus.ibmi.config.ConfigLoader;
import de.zeus.ibmi.connection.DriverManagerJdbcConnectionFactory;
import de.zeus.ibmi.output.OutputExportService;
import de.zeus.ibmi.output.OutputWriters;
import de.zeus.ibmi.runmanifest.InputHashCalculator;
import de.zeus.ibmi.runmanifest.RunManifestFactory;
import de.zeus.ibmi.runmanifest.RunManifest;
import de.zeus.ibmi.runmanifest.RunManifestJsonSerializer;
import de.zeus.ibmi.runmanifest.RunManifestWriter;
import de.zeus.ibmi.security.SecretMasker;
import de.zeus.ibmi.selection.ReadOnlyJdbcQueryExecutor;
import de.zeus.ibmi.selection.ReadOnlyQueryGuard;
import de.zeus.ibmi.selection.RunSelectionAndExportUseCase;
import de.zeus.ibmi.version.VersionProvider;
import java.io.PrintStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public final class CliApplication {

    private static final String TOOL_NAME = "zeus-ibmi-extract-transform";

    private final Map<String, String> environment;
    private final VersionProvider versionProvider;

    public CliApplication(Map<String, String> environment) {
        this(environment, new VersionProvider());
    }

    CliApplication(Map<String, String> environment, VersionProvider versionProvider) {
        this.environment = environment;
        this.versionProvider = versionProvider;
    }

    public int run(String[] args, PrintStream out, PrintStream err) {
        try {
            CliArguments parsed = CliArguments.parse(args == null ? new String[0] : args);
            String toolVersion = versionProvider.resolve();
            if (args == null || args.length == 0 || parsed.help()) {
                printHelp(out, toolVersion);
                return ExitCode.SUCCESS.code();
            }
            if (parsed.version()) {
                out.println(TOOL_NAME + " " + toolVersion);
                return ExitCode.SUCCESS.code();
            }
            if (parsed.configPath() == null) {
                throw new ConfigValidationException("Missing required argument: --config <file>");
            }
            return runWithConfig(parsed, out, toolVersion);
        } catch (RuntimeException ex) {
            ExitCode code = ExitCodeMapper.map(ex);
            err.println("Error: " + SecretMasker.maskSensitive(ex.getMessage()));
            return code.code();
        }
    }

    private int runWithConfig(CliArguments args, PrintStream out, String toolVersion) {
        AppConfig config;
        try {
            config = ConfigLoader.load(args.configPath(), environment, args.configOverrides());
        } catch (IOException ex) {
            throw new ConfigValidationException("Unable to load config file: " + args.configPath(), ex);
        }
        ReadOnlyQueryGuard guard = new ReadOnlyQueryGuard();
        String normalizedQuery = guard.validateOrNormalize(config.query());

        out.println("Configuration loaded from: " + args.configPath());
        out.println("Database URL: " + SecretMasker.maskSensitive(config.databaseUrl()));
        out.println("Database User: " + SecretMasker.maskSensitive(config.username()));
        out.println("Output Directory: " + config.outputDirectory());
        out.println("Output formats: " + String.join(",", config.outputFormatIds()));
        out.println("Run manifest enabled: " + config.runManifestEnabled());
        out.println("Read-only query check: OK");
        out.println("Query preview: " + previewQuery(normalizedQuery));

        if (!args.execute()) {
            Instant startedAt = Instant.now();
            Instant finishedAt = Instant.now();
            String runId = "run-" + UUID.randomUUID();
            RunManifest dryRunManifest = RunManifestFactory.dryRun(
                    TOOL_NAME,
                    toolVersion,
                    runId,
                    startedAt,
                    finishedAt,
                    args.configPath().toString(),
                    InputHashCalculator.sha256For(config, normalizedQuery),
                    previewQuery(normalizedQuery),
                    config.outputDirectory(),
                    config.outputFormatIds());
            out.println("Dry run only. Add --execute to run read-only query execution.");
            out.println("Planned output files: <run-id>." + String.join(", <run-id>.", config.outputFormatIds()));
            maybeWriteManifest(config, dryRunManifest, out);
            return ExitCode.SUCCESS.code();
        }

        RunSelectionAndExportUseCase useCase = new RunSelectionAndExportUseCase(
                new ReadOnlyJdbcQueryExecutor(
                        new DriverManagerJdbcConnectionFactory(),
                        guard),
                new OutputExportService(OutputWriters.defaultWriters()),
                TOOL_NAME,
                toolVersion);
        RunManifest manifest = useCase.run(config, args.configPath().toString(), normalizedQuery);
        maybeWriteManifest(config, manifest, out);
        out.println(RunManifestJsonSerializer.toJson(manifest));
        if ("SUCCESS".equals(manifest.status())) {
            return ExitCode.SUCCESS.code();
        }
        return ExitCodeMapper.mapErrorClassName(manifest.errorClass()).code();
    }

    private static void maybeWriteManifest(AppConfig config, RunManifest manifest, PrintStream out) {
        if (!config.runManifestEnabled()) {
            return;
        }
        Path manifestPath = new RunManifestWriter().write(Path.of(config.outputDirectory()), manifest);
        out.println("Run manifest written: " + manifestPath);
    }

    private static String previewQuery(String query) {
        if (query == null) {
            return "";
        }
        String singleLine = query.replace('\n', ' ').replace('\r', ' ').trim();
        int max = 160;
        if (singleLine.length() <= max) {
            return singleLine;
        }
        return singleLine.substring(0, max) + "...";
    }

    private static void printHelp(PrintStream out, String toolVersion) {
        out.println(TOOL_NAME + " " + toolVersion);
        out.println("Usage:");
        out.println("  --help | -h                  Show help");
        out.println("  --version | -v               Show version");
        out.println("  --config <file> [--execute]  Load config and optionally execute read-only query");
        out.println("  --db-url <url>               Override JDBC URL (CLI > ENV > file)");
        out.println("  --db-user <user>             Override JDBC user");
        out.println("  --db-password <pwd>          Override JDBC password");
        out.println("  --db-password-env <ENV_VAR>  Resolve password from named environment variable");
        out.println("  --query <sql>                Override SQL query");
        out.println("  --output-dir <path>          Override output directory");
        out.println("  --output-formats <csv>       Override output formats (xml,json,jsonl,csv,md)");
    }
}
