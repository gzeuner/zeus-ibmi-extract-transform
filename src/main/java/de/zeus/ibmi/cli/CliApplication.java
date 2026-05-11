package de.zeus.ibmi.cli;

import de.zeus.ibmi.config.AppConfig;
import de.zeus.ibmi.config.ConfigValidationException;
import de.zeus.ibmi.config.ConfigLoader;
import de.zeus.ibmi.connection.DriverManagerJdbcConnectionFactory;
import de.zeus.ibmi.output.OutputExportService;
import de.zeus.ibmi.output.OutputWriters;
import de.zeus.ibmi.query.QuerySource;
import de.zeus.ibmi.query.QuerySourceResolver;
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
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public final class CliApplication {

    private static final String TOOL_NAME = "zeus-ibmi-extract-transform";

    private final Map<String, String> environment;
    private final VersionProvider versionProvider;
    private final CliHelpRenderer helpRenderer;
    private final CliOutputRenderer outputRenderer;
    private final QuerySourceResolver querySourceResolver;

    public CliApplication(Map<String, String> environment) {
        this(environment, new VersionProvider(), new CliHelpRenderer(), new CliOutputRenderer(), new QuerySourceResolver());
    }

    CliApplication(Map<String, String> environment, VersionProvider versionProvider) {
        this(environment, versionProvider, new CliHelpRenderer(), new CliOutputRenderer(), new QuerySourceResolver());
    }

    CliApplication(
            Map<String, String> environment,
            VersionProvider versionProvider,
            CliHelpRenderer helpRenderer,
            CliOutputRenderer outputRenderer) {
        this(environment, versionProvider, helpRenderer, outputRenderer, new QuerySourceResolver());
    }

    CliApplication(
            Map<String, String> environment,
            VersionProvider versionProvider,
            CliHelpRenderer helpRenderer,
            CliOutputRenderer outputRenderer,
            QuerySourceResolver querySourceResolver) {
        this.environment = environment;
        this.versionProvider = versionProvider;
        this.helpRenderer = helpRenderer;
        this.outputRenderer = outputRenderer;
        this.querySourceResolver = querySourceResolver;
    }

    public int run(String[] args, PrintStream out, PrintStream err) {
        try {
            CliArguments parsed = CliArguments.parse(args == null ? new String[0] : args);
            String toolVersion = versionProvider.resolve();
            if (args == null || args.length == 0 || parsed.help()) {
                out.println(helpRenderer.render(TOOL_NAME, toolVersion));
                return ExitCode.SUCCESS.code();
            }
            if (parsed.version()) {
                out.println(TOOL_NAME + " " + toolVersion);
                return ExitCode.SUCCESS.code();
            }
            if (parsed.configPath() == null) {
                throw new CliArgumentException("Missing required argument: --config <file>");
            }
            return runWithConfig(parsed, out, toolVersion);
        } catch (RuntimeException ex) {
            ExitCode code = ExitCodeMapper.map(ex);
            err.println("Error: " + SecretMasker.maskSensitive(ex.getMessage()));
            return code.code();
        }
    }

    private int runWithConfig(CliArguments args, PrintStream out, String toolVersion) {
        AppConfig config = loadConfig(args);
        QuerySource querySource = querySourceResolver.resolve(config, args.configOverrides(), args.configPath());
        AppConfig resolvedConfig = config.withQuery(querySource.queryText());
        ReadOnlyQueryGuard guard = new ReadOnlyQueryGuard();
        String normalizedQuery = guard.validateOrNormalize(resolvedConfig.query());
        CliExecutionPlan plan = new CliExecutionPlan(
                args.configPath(),
                args.execute(),
                resolvedConfig.databaseUrl(),
                resolvedConfig.username(),
                resolvedConfig.outputDirectory(),
                resolvedConfig.outputFormatIds(),
                resolvedConfig.runManifestEnabled(),
                querySourceDisplay(querySource),
                querySource.multipleSourcesConfigured(),
                previewQuery(normalizedQuery));
        outputRenderer.renderExecutionPlan(out, plan);

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
                    querySource.sourceType().name(),
                    querySource.source(),
                    InputHashCalculator.sha256For(resolvedConfig, normalizedQuery),
                    previewQuery(normalizedQuery),
                    resolvedConfig.outputDirectory(),
                    resolvedConfig.outputFormatIds());
            outputRenderer.renderDryRunSummary(out, config.outputFormatIds());
            Path manifestPath = maybeWriteManifest(resolvedConfig, dryRunManifest);
            outputRenderer.renderManifestPath(out, manifestPath);
            return ExitCode.SUCCESS.code();
        }

        RunSelectionAndExportUseCase useCase = new RunSelectionAndExportUseCase(
                new ReadOnlyJdbcQueryExecutor(
                        new DriverManagerJdbcConnectionFactory(),
                        guard),
                new OutputExportService(OutputWriters.defaultWriters()),
                TOOL_NAME,
                toolVersion);
        RunManifest manifest = useCase.run(
                resolvedConfig,
                args.configPath().toString(),
                querySource.sourceType().name(),
                querySource.source(),
                normalizedQuery);
        Path manifestPath = maybeWriteManifest(resolvedConfig, manifest);
        outputRenderer.renderExecuteSummary(out, manifest);
        outputRenderer.renderManifestPath(out, manifestPath);
        out.println(RunManifestJsonSerializer.toJson(manifest));
        if ("SUCCESS".equals(manifest.status())) {
            return ExitCode.SUCCESS.code();
        }
        return ExitCodeMapper.mapErrorClassName(manifest.errorClass()).code();
    }

    private AppConfig loadConfig(CliArguments args) {
        try {
            return ConfigLoader.load(args.configPath(), environment, args.configOverrides());
        } catch (IOException ex) {
            throw new ConfigValidationException("Unable to load config file: " + args.configPath(), ex);
        }
    }

    private static Path maybeWriteManifest(AppConfig config, RunManifest manifest) {
        if (!config.runManifestEnabled()) {
            return null;
        }
        return new RunManifestWriter().write(Path.of(config.outputDirectory()), manifest);
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

    private static String querySourceDisplay(QuerySource querySource) {
        return switch (querySource.sourceType()) {
            case CLI_INLINE -> "CLI inline";
            case CLI_FILE -> "CLI file: " + querySource.source();
            case CONFIG_INLINE -> "Config inline";
            case CONFIG_FILE -> "Config file: " + querySource.source();
        };
    }

}
