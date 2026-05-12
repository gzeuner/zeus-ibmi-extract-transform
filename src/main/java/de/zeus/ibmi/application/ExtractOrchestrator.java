package de.zeus.ibmi.application;

import de.zeus.ibmi.domain.Query;
import de.zeus.ibmi.infrastructure.config.AppConfig;
import de.zeus.ibmi.infrastructure.output.OutputExportService;
import de.zeus.ibmi.infrastructure.security.SecurityUtils;
import de.zeus.ibmi.runmanifest.InputHashCalculator;
import de.zeus.ibmi.runmanifest.RunManifest;
import de.zeus.ibmi.selection.ReadOnlyJdbcQueryExecutor;
import de.zeus.ibmi.selection.ReadOnlyQueryGuard;
import de.zeus.ibmi.transform.QueryResult;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ExtractOrchestrator {

  private static final Logger LOG = LoggerFactory.getLogger(ExtractOrchestrator.class);

  private final ReadOnlyQueryGuard queryGuard;
  private final ReadOnlyJdbcQueryExecutor queryExecutor;
  private final OutputExportService outputExportService;
  private final RunManifestService runManifestService;
  private final String toolName;
  private final String toolVersion;

  public ExtractOrchestrator(
      ReadOnlyQueryGuard queryGuard,
      ReadOnlyJdbcQueryExecutor queryExecutor,
      OutputExportService outputExportService,
      RunManifestService runManifestService,
      String toolName,
      String toolVersion) {
    this.queryGuard = queryGuard;
    this.queryExecutor = queryExecutor;
    this.outputExportService = outputExportService;
    this.runManifestService = runManifestService;
    this.toolName = toolName;
    this.toolVersion = toolVersion;
  }

  public String validateQuery(String query) {
    return queryGuard.validateOrNormalize(query);
  }

  public RunManifest runDryRun(AppConfig config, Query query, String configSource) {
    Instant now = Instant.now();
    String runId = "run-" + UUID.randomUUID();
    return runManifestService.createDryRun(
        toolName,
        toolVersion,
        runId,
        now,
        Instant.now(),
        configSource,
        query.sourceType(),
        query.source(),
        InputHashCalculator.sha256For(config, query.sql()),
        query.preview(),
        config.outputDirectory(),
        config.outputFormatIds());
  }

  public RunManifest runExecute(AppConfig config, Query query, String configSource) {
    Instant startedAt = Instant.now();
    String runId = "run-" + UUID.randomUUID();
    try {
      QueryResult result = queryExecutor.execute(config.withQuery(query.sql()));
      List<Path> files =
          outputExportService.writeAll(
              result, Path.of(config.outputDirectory()), runId, config.outputFormatIds());
      RunManifest manifest =
          runManifestService.createSuccess(
              toolName,
              toolVersion,
              runId,
              startedAt,
              Instant.now(),
              configSource,
              query.sourceType(),
              query.source(),
              InputHashCalculator.sha256For(config, result.normalizedQuery()),
              query.preview(),
              Path.of(config.outputDirectory()),
              files,
              config.outputFormatIds(),
              result.rowCount(),
              result.columns().size());
      LOG.info(
          "Extract completed with {} rows and {} output files.", result.rowCount(), files.size());
      return manifest;
    } catch (RuntimeException ex) {
      LOG.error("Extract failed: {}", SecurityUtils.maskSecrets(ex.getMessage()));
      return runManifestService.createFailure(
          toolName,
          toolVersion,
          runId,
          startedAt,
          Instant.now(),
          configSource,
          query.sourceType(),
          query.source(),
          InputHashCalculator.sha256For(config, query.sql()),
          query.preview(),
          Path.of(config.outputDirectory()),
          List.of(),
          config.outputFormatIds(),
          ex,
          SecurityUtils.maskSecrets(ex.getMessage()));
    }
  }
}
