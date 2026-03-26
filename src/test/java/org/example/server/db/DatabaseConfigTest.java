package org.example.server.db;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseConfigTest {

    @Test
    void urlNoDb_isNotNull() {
        assertNotNull(DatabaseConfig.URL_NO_DB);
    }

    @Test
    void url_isNotNull() {
        assertNotNull(DatabaseConfig.URL);
    }

    @Test
    void urlNoDb_containsLocalhost() {
        assertTrue(DatabaseConfig.URL_NO_DB.contains("localhost"));
    }

    @Test
    void url_containsDbName() {
        assertTrue(DatabaseConfig.URL.contains(DatabaseConfig.DB_NAME));
    }

    @Test
    void url_startsWithJdbcMysql() {
        assertTrue(DatabaseConfig.URL.startsWith("jdbc:mysql://"));
    }
}
