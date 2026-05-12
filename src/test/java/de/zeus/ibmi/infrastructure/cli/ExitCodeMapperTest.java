package de.zeus.ibmi.infrastructure.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.zeus.ibmi.infrastructure.config.ConfigValidationException;
import de.zeus.ibmi.infrastructure.output.OutputWriteException;
import de.zeus.ibmi.runmanifest.ManifestException;
import de.zeus.ibmi.selection.QueryExecutionException;
import de.zeus.ibmi.selection.QueryGuardException;
import org.junit.jupiter.api.Test;

class ExitCodeMapperTest {

  @Test
  void map_shouldReturnExpectedCodes() {
    assertEquals(ExitCode.CONFIG_ERROR, ExitCodeMapper.map(new ConfigValidationException("x")));
    assertEquals(ExitCode.QUERY_GUARD_ERROR, ExitCodeMapper.map(new QueryGuardException("x")));
    assertEquals(ExitCode.JDBC_ERROR, ExitCodeMapper.map(new QueryExecutionException("x", null)));
    assertEquals(ExitCode.OUTPUT_ERROR, ExitCodeMapper.map(new OutputWriteException("x")));
    assertEquals(ExitCode.MANIFEST_ERROR, ExitCodeMapper.map(new ManifestException("x", null)));
    assertEquals(ExitCode.GENERAL_ERROR, ExitCodeMapper.map(new IllegalStateException("x")));
  }

  @Test
  void mapErrorClassName_shouldReturnExpectedCodes() {
    assertEquals(
        ExitCode.CONFIG_ERROR,
        ExitCodeMapper.mapErrorClassName(ConfigValidationException.class.getName()));
    assertEquals(
        ExitCode.QUERY_GUARD_ERROR,
        ExitCodeMapper.mapErrorClassName(QueryGuardException.class.getName()));
    assertEquals(
        ExitCode.JDBC_ERROR,
        ExitCodeMapper.mapErrorClassName(QueryExecutionException.class.getName()));
    assertEquals(
        ExitCode.OUTPUT_ERROR,
        ExitCodeMapper.mapErrorClassName(OutputWriteException.class.getName()));
    assertEquals(
        ExitCode.MANIFEST_ERROR,
        ExitCodeMapper.mapErrorClassName(ManifestException.class.getName()));
    assertEquals(
        ExitCode.GENERAL_ERROR,
        ExitCodeMapper.mapErrorClassName(IllegalStateException.class.getName()));
    assertEquals(ExitCode.GENERAL_ERROR, ExitCodeMapper.mapErrorClassName(""));
  }
}
