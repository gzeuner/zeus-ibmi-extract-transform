package de.zeus.ibmi.connection;

import java.sql.Connection;
import java.sql.SQLException;

public interface JdbcConnectionFactory {
    Connection open(String driverClassName, String jdbcUrl, String username, String password) throws SQLException;
}
