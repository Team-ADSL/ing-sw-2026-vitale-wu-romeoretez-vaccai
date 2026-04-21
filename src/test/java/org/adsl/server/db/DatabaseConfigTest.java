package org.adsl.server.db;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseConfigTest {

    @Test
    void testUrlNoDb_isNotNull() {
        assertNotNull(DatabaseConfig.URL_NO_DB);
    }

    @Test
    void testUrl_isNotNull() {
        assertNotNull(DatabaseConfig.URL);
    }

    @Test
    void testUrlNoDb_containsLocalhost() {
        assertTrue(DatabaseConfig.URL_NO_DB.contains("localhost"));
    }

    @Test
    void testUrl_containsDbName() {
        assertTrue(DatabaseConfig.URL.contains(DatabaseConfig.DB_NAME));
    }

    @Test
    void testUrl_startsWithJdbcMysql() {
        assertTrue(DatabaseConfig.URL.startsWith("jdbc:mysql://"));
    }
}
