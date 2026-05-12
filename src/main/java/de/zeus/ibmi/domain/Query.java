package de.zeus.ibmi.domain;

public record Query(
    String sql, String sourceType, String source, boolean multipleSourcesConfigured) {

  public Query {
    sql = sql == null ? "" : sql.trim();
    sourceType = sourceType == null ? "" : sourceType;
    source = source == null ? "" : source;
  }

  public String preview() {
    if (sql.isBlank()) {
      return "";
    }
    String singleLine = sql.replace('\n', ' ').replace('\r', ' ').trim();
    return singleLine.length() <= 180 ? singleLine : singleLine.substring(0, 180) + "...";
  }
}
