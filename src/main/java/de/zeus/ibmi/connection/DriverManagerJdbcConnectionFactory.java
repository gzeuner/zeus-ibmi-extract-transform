package de.zeus.ibmi.connection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DriverManagerJdbcConnectionFactory implements JdbcConnectionFactory {

    @Override
    public Connection open(String driverClassName, String jdbcUrl, String username, String password) throws SQLException {
        if (driverClassName != null && !driverClassName.isBlank()) {
            try {
                Class.forName(driverClassName);
            } catch (ClassNotFoundException e) {
                throw new SQLException("JDBC driver class not found: " + driverClassName, e);
            }
        }
        return DriverManager.getConnection(jdbcUrl, username, password);
    }
}
