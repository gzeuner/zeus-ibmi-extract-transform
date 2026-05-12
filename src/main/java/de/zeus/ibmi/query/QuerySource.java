package de.zeus.ibmi.query;

public record QuerySource(
    QuerySourceType sourceType,
    String source,
    String queryText,
    boolean multipleSourcesConfigured) {

  public QuerySource {
    source = source == null ? "" : source;
    queryText = queryText == null ? "" : queryText;
  }
}
