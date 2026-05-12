package de.zeus.ibmi.infrastructure.config;

public final class ConfigCompatibilityMapper {

  private ConfigCompatibilityMapper() {}

  public static ZeusIbmiProperties from(AppConfig config) {
    ZeusIbmiProperties properties = new ZeusIbmiProperties();
    ZeusIbmiProperties.Db db = new ZeusIbmiProperties.Db();
    db.setDriver(config.databaseDriver());
    db.setUrl(config.databaseUrl());
    db.setUser(config.username());
    db.setPassword(config.password());
    db.setPasswordEnv(config.passwordEnvName());
    db.setAllowEmptyPassword(config.allowEmptyPassword());

    ZeusIbmiProperties.Query query = new ZeusIbmiProperties.Query();
    query.setSql(config.query());
    query.setFile(config.queryFile());
    query.setFetchSize(config.fetchSize());
    query.setTimeoutSeconds(config.queryTimeoutSeconds());

    ZeusIbmiProperties.Output output = new ZeusIbmiProperties.Output();
    output.setDirectory(config.outputDirectory());
    output.setFormats(config.outputFormatIds());
    output.setHtml(
        new ZeusIbmiProperties.Output.HtmlProperties(
            config.htmlTheme(), config.htmlCustomCssFile(), config.htmlIncludeManifest()));

    ZeusIbmiProperties.Manifest manifest = new ZeusIbmiProperties.Manifest();
    manifest.setEnabled(config.runManifestEnabled());

    properties.setDb(db);
    properties.setQuery(query);
    properties.setOutput(output);
    properties.setManifest(manifest);
    return properties;
  }
}
