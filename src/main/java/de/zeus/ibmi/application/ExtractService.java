package de.zeus.ibmi.application;

import de.zeus.ibmi.domain.Query;
import de.zeus.ibmi.infrastructure.config.AppConfig;
import de.zeus.ibmi.runmanifest.RunManifest;

public final class ExtractService {

  private final ExtractOrchestrator orchestrator;

  public ExtractService(ExtractOrchestrator orchestrator) {
    this.orchestrator = orchestrator;
  }

  public RunManifest run(AppConfig config, Query query, boolean execute, String configSource) {
    if (execute) {
      return orchestrator.runExecute(config, query, configSource);
    }
    return orchestrator.runDryRun(config, query, configSource);
  }
}
