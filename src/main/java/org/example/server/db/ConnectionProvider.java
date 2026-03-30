package org.example.server.db;

import java.sql.Connection;

@FunctionalInterface
public interface ConnectionProvider {
    Connection getConnection();
}
