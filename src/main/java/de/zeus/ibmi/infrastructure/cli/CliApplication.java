package de.zeus.ibmi.infrastructure.cli;

import de.zeus.ibmi.application.ExtractOrchestrator;
import de.zeus.ibmi.application.ExtractService;
import de.zeus.ibmi.application.RunManifestService;
import de.zeus.ibmi.common.version.VersionProvider;
import de.zeus.ibmi.connection.DriverManagerJdbcConnectionFactory;
import de.zeus.ibmi.domain.Query;
import de.zeus.ibmi.infrastructure.config.AppConfig;
import de.zeus.ibmi.infrastructure.config.ConfigCompatibilityMapper;
import de.zeus.ibmi.infrastructure.config.ConfigLoader;
import de.zeus.ibmi.infrastructure.config.ConfigValidationException;
import de.zeus.ibmi.infrastructure.config.ConfigValidator;
import de.zeus.ibmi.infrastructure.output.OutputExportService;
import de.zeus.ibmi.infrastructure.output.OutputWriterRegistry;
import de.zeus.ibmi.infrastructure.security.SecurityUtils;
import de.zeus.ibmi.query.QuerySource;
import de.zeus.ibmi.query.QuerySourceResolver;
import de.zeus.ibmi.runmanifest.RunManifest;
import de.zeus.ibmi.runmanifest.RunManifestJsonSerializer;
import de.zeus.ibmi.selection.ReadOnlyQueryGuard;
import jakarta.validation.Validation;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CliApplication {

  private static final String TOOL_NAME = "zeus-ibmi-extract-transform";
  private static final Logger LOG = LoggerFactory.getLogger(CliApplication.class);

  private final Map<String, String> environment;
  private final VersionProvider versionProvider;
  private final CliHelpRenderer helpRenderer;
  private final CliOutputRenderer outputRenderer;
  private final QuerySourceResolver querySourceResolver;

  public CliApplication(Map<String, String> environment) {
    this(
        environment,
        new VersionProvider(),
        new CliHelpRenderer(),
        new CliOutputRenderer(),
        new QuerySourceResolver());
  }

  CliApplication(Map<String, String> environment, VersionProvider versionProvider) {
    this(
        environment,
        versionProvider,
        new CliHelpRenderer(),
        new CliOutputRenderer(),
        new QuerySourceResolver());
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
      err.println("Error: " + SecurityUtils.maskSecrets(ex.getMessage()));
      LOG.error(
          "CLI execution failed with code {}: {}",
          code.code(),
          SecurityUtils.maskSecrets(ex.getMessage()));
      return code.code();
    }
  }

  private int runWithConfig(CliArguments args, PrintStream out, String toolVersion) {
    AppConfig config = loadConfig(args);
    validateWithSpringStyleProperties(config);
    QuerySource querySource =
        querySourceResolver.resolve(config, args.configOverrides(), args.configPath());
    AppConfig resolvedConfig = config.withQuery(querySource.queryText());
    RunManifestService runManifestService = new RunManifestService();
    ExtractOrchestrator orchestrator = buildOrchestrator(toolVersion, runManifestService);
    String normalizedQuery = orchestrator.validateQuery(resolvedConfig.query());
    Query query =
        new Query(
            normalizedQuery,
            querySource.sourceType().name(),
            querySource.source(),
            querySource.multipleSourcesConfigured());
    CliExecutionPlan plan =
        new CliExecutionPlan(
            args.configPath(),
            args.execute(),
            resolvedConfig.databaseUrl(),
            resolvedConfig.username(),
            resolvedConfig.outputDirectory(),
            resolvedConfig.outputFormatIds(),
            resolvedConfig.runManifestEnabled(),
            querySourceDisplay(querySource),
            querySource.multipleSourcesConfigured(),
            query.preview());
    outputRenderer.renderExecutionPlan(out, plan);

    ExtractService extractService = new ExtractService(orchestrator);
    RunManifest manifest =
        extractService.run(resolvedConfig, query, args.execute(), args.configPath().toString());
    if (!args.execute()) {
      outputRenderer.renderDryRunSummary(out, config.outputFormatIds());
      Path manifestPath = maybeWriteManifest(runManifestService, resolvedConfig, manifest);
      outputRenderer.renderManifestPath(out, manifestPath);
      return ExitCode.SUCCESS.code();
    }

    Path manifestPath = maybeWriteManifest(runManifestService, resolvedConfig, manifest);
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

  private ExtractOrchestrator buildOrchestrator(
      String toolVersion, RunManifestService runManifestService) {
    ReadOnlyQueryGuard guard = new ReadOnlyQueryGuard();
    return new ExtractOrchestrator(
        guard,
        new de.zeus.ibmi.selection.ReadOnlyJdbcQueryExecutor(
            new DriverManagerJdbcConnectionFactory(), guard),
        new OutputExportService(OutputWriterRegistry.defaultRegistry()),
        runManifestService,
        TOOL_NAME,
        toolVersion);
  }

  private static Path maybeWriteManifest(
      RunManifestService runManifestService, AppConfig config, RunManifest manifest) {
    if (!config.runManifestEnabled()) {
      return null;
    }
    return runManifestService.writeManifest(Path.of(config.outputDirectory()), manifest);
  }

  private static void validateWithSpringStyleProperties(AppConfig config) {
    ConfigValidator configValidator =
        new ConfigValidator(Validation.buildDefaultValidatorFactory().getValidator());
    configValidator.validate(ConfigCompatibilityMapper.from(config));
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
