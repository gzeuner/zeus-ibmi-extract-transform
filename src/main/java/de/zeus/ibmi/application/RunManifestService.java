package de.zeus.ibmi.application;

import de.zeus.ibmi.runmanifest.RunManifest;
import de.zeus.ibmi.runmanifest.RunManifestFactory;
import de.zeus.ibmi.runmanifest.RunManifestWriter;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

public final class RunManifestService {

  private final RunManifestWriter writer;

  public RunManifestService() {
    this(new RunManifestWriter());
  }

  public RunManifestService(RunManifestWriter writer) {
    this.writer = writer;
  }

  public RunManifest createDryRun(
      String toolName,
      String toolVersion,
      String runId,
      Instant startedAt,
      Instant finishedAt,
      String configSource,
      String querySourceType,
      String querySource,
      String queryHash,
      String queryPreview,
      String outputDirectory,
      List<String> outputFormats) {
    return RunManifestFactory.dryRun(
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
        outputDirectory,
        outputFormats);
  }

  public RunManifest createSuccess(
      String toolName,
      String toolVersion,
      String runId,
      Instant startedAt,
      Instant finishedAt,
      String configSource,
      String querySourceType,
      String querySource,
      String queryHash,
      String queryPreview,
      Path outputDirectory,
      List<Path> outputFiles,
      List<String> outputFormats,
      int rowCount,
      int columnCount) {
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
        outputDirectory,
        outputFiles,
        outputFormats,
        rowCount,
        columnCount);
  }

  public RunManifest createFailure(
      String toolName,
      String toolVersion,
      String runId,
      Instant startedAt,
      Instant finishedAt,
      String configSource,
      String querySourceType,
      String querySource,
      String queryHash,
      String queryPreview,
      Path outputDirectory,
      List<Path> outputFiles,
      List<String> outputFormats,
      Throwable error,
      String sanitizedErrorMessage) {
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
        outputDirectory,
        outputFiles,
        outputFormats,
        error,
        sanitizedErrorMessage);
  }

  public Path writeManifest(Path outputDirectory, RunManifest manifest) {
    return writer.write(outputDirectory, manifest);
  }
}
