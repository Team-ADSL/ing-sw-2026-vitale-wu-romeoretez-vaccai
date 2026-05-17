package org.adsl.server.db;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class DatabaseManagerTest {

    @Test
    void testInitDatabase_createsTablesWithoutError() {
        assumeTrue(System.getenv("DB_PASSWORD") != null, "DB_PASSWORD not set — skipping DB tests");
        DatabaseManager.initDatabase();
    }

    @Test
    void testInitDatabase_isIdempotent() {
        assumeTrue(System.getenv("DB_PASSWORD") != null, "DB_PASSWORD not set — skipping DB tests");
        DatabaseManager.initDatabase();
        DatabaseManager.initDatabase();
    }

    @Test
    void testInitDatabase_throwsIfPasswordNull() {
        assumeTrue(System.getenv("DB_PASSWORD") == null, "DB_PASSWORD is set — this test requires it to be unset");
        assertThrows(RuntimeException.class, DatabaseManager::initDatabase);
    }
}
