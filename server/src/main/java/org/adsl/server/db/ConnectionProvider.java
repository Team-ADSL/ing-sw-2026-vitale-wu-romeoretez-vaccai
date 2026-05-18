package org.adsl.server.db;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Functional interface for obtaining a JDBC {@link Connection}.
 * <p>
 * The production implementation is {@link DatabaseConfig#getConnection()}.
 * Using this interface instead of a direct static call allows tests to inject
 * an in-memory or mock connection without touching global state.
 * </p>
 */
@FunctionalInterface
public interface ConnectionProvider {
    /**
     * Returns a live JDBC connection. The caller is responsible for closing it.
     *
     * @return an open {@link Connection}
     * @throws SQLException if the connection cannot be established
     */
    Connection getConnection() throws SQLException;
}
