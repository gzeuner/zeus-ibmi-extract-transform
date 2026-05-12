# zeus-ibmi-extract-transform

English | [Deutsch](README.de.md)

`zeus-ibmi-extract-transform` is a safety-first Java CLI for read-only SQL extraction from IBM i (DB2 for i) with deterministic multi-format exports and a full `RunManifest` audit trail.

## Status
Version `0.2.0` (production-hardening baseline).

## Core Safety Model
- Dry-run is default; `--execute` is required for real query execution.
- Read-only guard allows `SELECT` / `WITH` only and blocks multi-statement and dangerous keywords.
- Secrets are masked in CLI errors and manifest error fields.
- Query source precedence is deterministic: `--query` > `--query-file` > `query.sql` > `query.file`.
- Configuration precedence is deterministic: `CLI` > `ENV` > config file > defaults.

## Requirements
- Java 21+
- Maven 3.9+

## Build
```bash
./mvnw verify
```

## Quickstart
Dry-run:
```bash
java -jar target/zeus-ibmi-extract-transform-0.2.0.jar --config config/example.application.properties
```

Execute:
```bash
java -jar target/zeus-ibmi-extract-transform-0.2.0.jar --config config/example.application.properties --execute
```

Execute with all output formats:
```bash
java -jar target/zeus-ibmi-extract-transform-0.2.0.jar \
  --config config/example.application.properties \
  --output-formats xml,json,jsonl,csv,md \
  --execute
```

H2 local demo:
```bash
java -jar target/zeus-ibmi-extract-transform-0.2.0.jar \
  --config config/example.application.properties \
  --db-driver org.h2.Driver \
  --db-url "jdbc:h2:mem:demo_readonly;MODE=DB2;DB_CLOSE_DELAY=-1" \
  --query "SELECT 1 AS ID, 'demo' AS NAME" \
  --execute
```

## CLI
```text
General:
  -h, --help
  -v, --version

Config:
  --config <file>

Database:
  --db-driver <class>
  --db-url <url>
  --db-user <user>
  --db-password <pwd>
  --db-password-env <ENV_VAR>
  --allow-empty-password

Query:
  --query <sql>
  --query-file <file>
  --fetch-size <n>
  --query-timeout-seconds <n>

Output:
  --output-dir <path>
  --output-formats <csv>
  --manifest-enabled
  --manifest-disabled

Execution:
  -x, --execute
  --dry-run
```

## Configuration
Supported config file types:
- `.properties` (legacy-compatible)
- `.yml` / `.yaml` (Spring-style `zeus.ibmi.*` keys)

Environment mappings:
- `ZEUS_IBMI_DB_URL`
- `ZEUS_IBMI_DB_USER`
- `ZEUS_IBMI_DB_PASSWORD`
- `ZEUS_IBMI_DB_PASSWORD_ENV`
- `ZEUS_IBMI_OUTPUT_DIRECTORY`
- `ZEUS_IBMI_OUTPUT_FORMATS`
- `ZEUS_IBMI_RUN_MANIFEST_ENABLED`
- `ZEUS_IBMI_QUERY_FETCH_SIZE`
- `ZEUS_IBMI_QUERY_TIMEOUT_SECONDS`

See [`docs/output-formats.md`](docs/output-formats.md), [`docs/ibmi-jt400-notes.md`](docs/ibmi-jt400-notes.md), and [`ARCHITECTURE.md`](ARCHITECTURE.md).

## Exit Codes
- `0` success
- `1` general
- `2` config/arguments
- `3` query guard
- `4` JDBC/SQL
- `5` output/filesystem
- `6` manifest

## Security Notes
- Prefer environment variables for credentials.
- Avoid passing passwords on the command line in shared or logged shells.
- RunManifest intentionally avoids absolute sensitive local paths where possible.

## Disclaimer
Not an IBM product. Validate configuration, security controls, and output correctness before production use.