package de.zeus.ibmi.selection;

import de.zeus.ibmi.config.AppConfig;
import de.zeus.ibmi.output.OutputExportService;
import de.zeus.ibmi.output.OutputWriteException;
import de.zeus.ibmi.runmanifest.InputHashCalculator;
import de.zeus.ibmi.runmanifest.RunManifest;
import de.zeus.ibmi.runmanifest.RunManifestFactory;
import de.zeus.ibmi.security.SecretMasker;
import de.zeus.ibmi.transform.QueryResult;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class RunSelectionAndExportUseCase {

    private final ReadOnlyJdbcQueryExecutor queryExecutor;
    private final OutputExportService outputExportService;
    private final String toolName;
    private final String toolVersion;

    public RunSelectionAndExportUseCase(
            ReadOnlyJdbcQueryExecutor queryExecutor,
            OutputExportService outputExportService,
            String toolName,
            String toolVersion) {
        this.queryExecutor = queryExecutor;
        this.outputExportService = outputExportService;
        this.toolName = toolName;
        this.toolVersion = toolVersion;
    }

    public RunManifest run(
            AppConfig config,
            String configSource,
            String querySourceType,
            String querySource,
            String normalizedQuery) {
        Instant startedAt = Instant.now();
        String runId = "run-" + UUID.randomUUID();
        String safeNormalizedQuery = normalizedQuery == null ? "" : normalizedQuery;
        String queryHash = InputHashCalculator.sha256For(config, safeNormalizedQuery);
        String queryPreview = previewQuery(safeNormalizedQuery);

        try {
            QueryResult result = queryExecutor.execute(config);
            queryHash = InputHashCalculator.sha256For(config, result.normalizedQuery());
            queryPreview = previewQuery(result.normalizedQuery());

            Path outputDir = Path.of(config.outputDirectory());
            List<Path> files = outputExportService.writeAll(
                    result,
                    outputDir,
                    runId,
                    config.outputFormatIds());

            Instant finishedAt = Instant.now();
            return RunManifestFactory.success(
                    toolName,
                    toolVersion,
                    runId,
                    startedAt,
                    finishedAt,
                    configSource,
                    querySourceType,
                    querySource,
                    queryHash,
                    queryPreview,
                    outputDir,
                    files,
                    config.outputFormatIds(),
                    result.rowCount(),
                    result.columns().size());
        } catch (RuntimeException ex) {
            Instant finishedAt = Instant.now();
            List<Path> outputFiles = ex instanceof OutputWriteException outputWriteException
                    ? outputWriteException.writtenFiles()
                    : List.of();
            return RunManifestFactory.failure(
                    toolName,
                    toolVersion,
                    runId,
                    startedAt,
                    finishedAt,
                    configSource,
                    querySourceType,
                    querySource,
                    queryHash,
                    queryPreview,
                    Path.of(config.outputDirectory()),
                    outputFiles,
                    config.outputFormatIds(),
                    ex,
                    SecretMasker.maskSensitive(ex.getMessage()));
        }
    }

    private static String previewQuery(String normalizedQuery) {
        if (normalizedQuery == null) {
            return "";
        }
        String oneLine = normalizedQuery.replace('\n', ' ').replace('\r', ' ').trim();
        return oneLine.length() <= 180 ? oneLine : oneLine.substring(0, 180) + "...";
    }
}
