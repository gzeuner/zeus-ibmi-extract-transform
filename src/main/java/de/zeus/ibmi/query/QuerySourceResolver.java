package de.zeus.ibmi.query;

import de.zeus.ibmi.config.AppConfig;
import de.zeus.ibmi.config.ConfigValidationException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class QuerySourceResolver {

    private final QueryFileLoader queryFileLoader;

    public QuerySourceResolver() {
        this(new QueryFileLoader());
    }

    QuerySourceResolver(QueryFileLoader queryFileLoader) {
        this.queryFileLoader = queryFileLoader;
    }

    public QuerySource resolve(AppConfig config, Map<String, String> cliOverrides, Path configPath) {
        String cliQuery = trimToNull(value(cliOverrides, "query.sql"));
        String cliQueryFile = trimToNull(value(cliOverrides, "query.file"));
        String configQuery = trimToNull(config.query());
        String configQueryFile = trimToNull(config.queryFile());

        List<QuerySourceType> configuredSources = configuredSources(cliQuery, cliQueryFile, configQuery, configQueryFile);
        if (configuredSources.isEmpty()) {
            throw new ConfigValidationException("query.sql or query.file is required");
        }

        if (cliQuery != null) {
            return new QuerySource(
                    QuerySourceType.CLI_INLINE,
                    "CLI inline",
                    cliQuery,
                    configuredSources.size() > 1);
        }
        if (cliQueryFile != null) {
            Path filePath = Path.of(cliQueryFile);
            String queryText = queryFileLoader.load(filePath);
            return new QuerySource(
                    QuerySourceType.CLI_FILE,
                    safeQuerySourceDisplay(cliQueryFile),
                    queryText,
                    configuredSources.size() > 1);
        }
        if (configQuery != null) {
            return new QuerySource(
                    QuerySourceType.CONFIG_INLINE,
                    "Config inline",
                    configQuery,
                    configuredSources.size() > 1);
        }

        Path filePath = resolveConfigQueryFilePath(configQueryFile, configPath);
        String queryText = queryFileLoader.load(filePath);
        return new QuerySource(
                QuerySourceType.CONFIG_FILE,
                safeQuerySourceDisplay(configQueryFile),
                queryText,
                configuredSources.size() > 1);
    }

    private static Path resolveConfigQueryFilePath(String queryFile, Path configPath) {
        Path configuredPath = Path.of(queryFile);
        if (configuredPath.isAbsolute()) {
            return configuredPath;
        }
        Path absoluteConfig = configPath == null ? null : configPath.toAbsolutePath().normalize();
        Path configDirectory = absoluteConfig == null ? null : absoluteConfig.getParent();
        if (configDirectory == null) {
            return configuredPath;
        }
        return configDirectory.resolve(configuredPath).normalize();
    }

    private static List<QuerySourceType> configuredSources(
            String cliQuery,
            String cliQueryFile,
            String configQuery,
            String configQueryFile) {
        List<QuerySourceType> sources = new ArrayList<>();
        if (cliQuery != null) {
            sources.add(QuerySourceType.CLI_INLINE);
        }
        if (cliQueryFile != null) {
            sources.add(QuerySourceType.CLI_FILE);
        }
        if (configQuery != null) {
            sources.add(QuerySourceType.CONFIG_INLINE);
        }
        if (configQueryFile != null) {
            sources.add(QuerySourceType.CONFIG_FILE);
        }
        return List.copyOf(sources);
    }

    private static String safeQuerySourceDisplay(String configuredPath) {
        if (configuredPath == null || configuredPath.isBlank()) {
            return "<query-file>";
        }
        String normalized = configuredPath.replace('\\', '/').trim();
        Path fileName = Path.of(configuredPath).getFileName();
        if (fileName != null && !fileName.toString().isBlank()) {
            return fileName.toString().replace('\\', '/');
        }
        if (looksAbsolute(normalized)) {
            return "<query-file>";
        }
        return normalized;
    }

    private static boolean looksAbsolute(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        return value.startsWith("/")
                || value.startsWith("//")
                || value.matches("^[A-Za-z]:[\\\\/].*");
    }

    private static String value(Map<String, String> map, String key) {
        return map == null ? null : map.get(key);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
