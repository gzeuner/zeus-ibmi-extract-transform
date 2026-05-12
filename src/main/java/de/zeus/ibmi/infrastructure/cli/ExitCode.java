package de.zeus.ibmi.infrastructure.cli;

public enum ExitCode {
  SUCCESS(0),
  GENERAL_ERROR(1),
  CONFIG_ERROR(2),
  QUERY_GUARD_ERROR(3),
  JDBC_ERROR(4),
  OUTPUT_ERROR(5),
  MANIFEST_ERROR(6);

  private final int code;

  ExitCode(int code) {
    this.code = code;
  }

  public int code() {
    return code;
  }
}
