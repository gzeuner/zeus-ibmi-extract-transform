package de.zeus.ibmi.transform;

public record ColumnDefinition(
        String name,
        int jdbcType,
        String jdbcTypeName) {
}
