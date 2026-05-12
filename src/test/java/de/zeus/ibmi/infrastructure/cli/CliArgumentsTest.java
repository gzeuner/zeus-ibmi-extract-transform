package de.zeus.ibmi.infrastructure.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CliArgumentsTest {

  @Test
  void parse_shouldReadAllFlagsAndOverrides() {
    CliArguments args =
        CliArguments.parse(
            new String[] {
              "--config",
              "app.properties",
              "--execute",
              "--db-driver",
              "org.h2.Driver",
              "--db-url",
              "jdbc:h2:mem:test",
              "--db-user",
              "sa",
              "--db-password",
              "secret",
              "--db-password-env",
              "DB_PASSWORD",
              "--query",
              "SELECT 1",
              "--query-file",
              "queries/customers.sql",
              "--output-dir",
              "target/out",
              "--output-formats",
              "json,csv",
              "--manifest-disabled",
              "--fetch-size",
              "123",
              "--query-timeout-seconds",
              "45",
              "--allow-empty-password"
            });

    assertEquals("app.properties", args.configPath().toString());
    assertTrue(args.execute());
    assertFalse(args.help());
    assertFalse(args.version());
    assertEquals("org.h2.Driver", args.configOverrides().get("db.driver"));
    assertEquals("jdbc:h2:mem:test", args.configOverrides().get("db.url"));
    assertEquals("sa", args.configOverrides().get("db.user"));
    assertEquals("secret", args.configOverrides().get("db.password"));
    assertEquals("DB_PASSWORD", args.configOverrides().get("db.passwordEnv"));
    assertEquals("SELECT 1", args.configOverrides().get("query.sql"));
    assertEquals("queries/customers.sql", args.configOverrides().get("query.file"));
    assertEquals("target/out", args.configOverrides().get("output.directory"));
    assertEquals("json,csv", args.configOverrides().get("output.formats"));
    assertEquals("false", args.configOverrides().get("run.manifest.enabled"));
    assertEquals("123", args.configOverrides().get("query.fetchSize"));
    assertEquals("45", args.configOverrides().get("query.timeoutSeconds"));
    assertEquals("true", args.configOverrides().get("db.allowEmptyPassword"));
  }

  @Test
  void parse_shouldHandleHelpAndVersionFlags() {
    CliArguments help = CliArguments.parse(new String[] {"--help"});
    CliArguments version = CliArguments.parse(new String[] {"--version"});
    CliArguments shortFlags = CliArguments.parse(new String[] {"-h", "-v", "-x"});

    assertTrue(help.help());
    assertFalse(help.version());
    assertTrue(version.version());
    assertFalse(version.help());
    assertTrue(shortFlags.help());
    assertTrue(shortFlags.version());
    assertTrue(shortFlags.execute());
  }

  @Test
  void parse_shouldFailOnMissingValue() {
    CliArgumentException ex =
        assertThrows(
            CliArgumentException.class, () -> CliArguments.parse(new String[] {"--config"}));
    assertEquals("Missing value for --config", ex.getMessage());
  }

  @Test
  void parse_shouldFailOnOptionAsValue() {
    CliArgumentException ex =
        assertThrows(
            CliArgumentException.class,
            () -> CliArguments.parse(new String[] {"--config", "--execute"}));
    assertEquals("Missing value for --config", ex.getMessage());
  }

  @Test
  void parse_shouldFailWhenQueryFileHasMissingValue() {
    CliArgumentException ex =
        assertThrows(
            CliArgumentException.class, () -> CliArguments.parse(new String[] {"--query-file"}));
    assertEquals("Missing value for --query-file", ex.getMessage());
  }

  @Test
  void parse_shouldFailWhenQueryFileValueLooksLikeOption() {
    CliArgumentException ex =
        assertThrows(
            CliArgumentException.class,
            () -> CliArguments.parse(new String[] {"--query-file", "--execute"}));
    assertEquals("Missing value for --query-file", ex.getMessage());
  }

  @Test
  void parse_shouldFailOnUnknownOption() {
    CliArgumentException ex =
        assertThrows(
            CliArgumentException.class, () -> CliArguments.parse(new String[] {"--unknown"}));
    assertEquals("Unknown argument: --unknown. Use --help for usage.", ex.getMessage());
  }

  @Test
  void parse_shouldUseLastValueForDuplicateOptions() {
    CliArguments args =
        CliArguments.parse(
            new String[] {
              "--config",
              "first.properties",
              "--config",
              "second.properties",
              "--db-url",
              "jdbc:h2:mem:first",
              "--db-url",
              "jdbc:h2:mem:second",
              "--manifest-enabled",
              "--manifest-disabled"
            });

    assertEquals("second.properties", args.configPath().toString());
    assertEquals("jdbc:h2:mem:second", args.configOverrides().get("db.url"));
    assertEquals("false", args.configOverrides().get("run.manifest.enabled"));
  }
}
