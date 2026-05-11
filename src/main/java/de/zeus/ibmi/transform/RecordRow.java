package de.zeus.ibmi.transform;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Collections;

public record RecordRow(Map<String, Object> values) {

    public RecordRow {
        values = values == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    public Object value(String columnName) {
        return values.get(columnName);
    }
}
