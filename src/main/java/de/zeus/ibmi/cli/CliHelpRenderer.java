package de.zeus.ibmi.cli;

public final class CliHelpRenderer {

    public String render(String toolName, String toolVersion) {
        StringBuilder help = new StringBuilder();
        help.append(toolName).append(" ").append(toolVersion).append("\n\n");
        help.append("CLI-first read-only DB2 for i extract/transform tool.\n\n");
        help.append("Usage:\n");
        help.append("  java -jar ").append(toolName).append(".jar --config config/example.application.properties\n");
        help.append("  java -jar ").append(toolName).append(".jar --config config/example.application.properties --execute\n");
        help.append("  java -jar ").append(toolName)
                .append(".jar --config config/example.application.properties --output-formats xml,json,jsonl,csv,md --execute\n");
        help.append("  java -jar ").append(toolName)
                .append(".jar --config config/example.application.properties --db-driver org.h2.Driver --db-url \"jdbc:h2:mem:demo_readonly;MODE=DB2;DB_CLOSE_DELAY=-1\" --query \"SELECT 1 AS ID\" --execute\n\n");
        help.append("  java -jar ").append(toolName)
                .append(".jar --config config/example.application.properties --query-file queries/customers.sql --execute\n\n");
        help.append("General:\n");
        help.append("  -h, --help                      Show help\n");
        help.append("  -v, --version                   Show version\n\n");
        help.append("Config:\n");
        help.append("  --config <file>                 Config file path\n\n");
        help.append("Database:\n");
        help.append("  --db-driver <class>             Override JDBC driver\n");
        help.append("  --db-url <url>                  Override JDBC URL (CLI > ENV > file)\n");
        help.append("  --db-user <user>                Override JDBC user\n");
        help.append("  --db-password <pwd>             Override JDBC password\n");
        help.append("  --db-password-env <ENV_VAR>     Resolve password from named environment variable\n");
        help.append("  --allow-empty-password          Allow empty db.password (for local/offline scenarios)\n\n");
        help.append("Query:\n");
        help.append("  --query <sql>                   Override SQL query\n");
        help.append("  --query-file <file>             Override SQL query file (UTF-8)\n");
        help.append("  --fetch-size <n>                Override JDBC fetch size\n");
        help.append("  --query-timeout-seconds <n>     Override JDBC query timeout in seconds\n\n");
        help.append("Output:\n");
        help.append("  --output-dir <path>             Override output directory\n");
        help.append("  --output-formats <csv>          Override output formats (xml,json,jsonl,csv,md)\n");
        help.append("  --manifest-enabled              Force run manifest enabled\n");
        help.append("  --manifest-disabled             Force run manifest disabled\n\n");
        help.append("Execution:\n");
        help.append("  -x, --execute                   Execute read-only query (default is dry-run)\n\n");
        help.append("Notes:\n");
        help.append("  - Duplicate CLI options are allowed; the last value wins.\n");
        help.append("  - Prefer ENV-based secrets; use --db-password only consciously for local use.\n");
        help.append("  - Exit codes: 0 success, 1 general, 2 config/args, 3 guard, 4 jdbc, 5 output, 6 manifest.\n");
        return help.toString();
    }
}
