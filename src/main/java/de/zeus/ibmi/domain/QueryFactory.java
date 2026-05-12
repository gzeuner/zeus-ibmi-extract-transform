package de.zeus.ibmi.domain;

import de.zeus.ibmi.infrastructure.config.AppConfig;
import de.zeus.ibmi.query.QuerySource;
import de.zeus.ibmi.query.QuerySourceResolver;
import java.nio.file.Path;
import java.util.Map;

public final class QueryFactory {

  private final QuerySourceResolver querySourceResolver;

  public QueryFactory() {
    this(new QuerySourceResolver());
  }

  public QueryFactory(QuerySourceResolver querySourceResolver) {
    this.querySourceResolver = querySourceResolver;
  }

  public Query resolve(
      AppConfig config, Map<String, String> cliOverrides, Path configPath, String normalizedSql) {
    QuerySource querySource = querySourceResolver.resolve(config, cliOverrides, configPath);
    return new Query(
        normalizedSql,
        querySource.sourceType().name(),
        querySource.source(),
        querySource.multipleSourcesConfigured());
  }
}
