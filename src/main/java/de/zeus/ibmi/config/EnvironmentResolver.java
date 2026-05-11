package de.zeus.ibmi.config;

import java.util.Map;

public final class EnvironmentResolver {

    private final Map<String, String> env;

    public EnvironmentResolver(Map<String, String> env) {
        this.env = env;
    }

    public String get(String key) {
        if (env == null) {
            return null;
        }
        return env.get(key);
    }
}
