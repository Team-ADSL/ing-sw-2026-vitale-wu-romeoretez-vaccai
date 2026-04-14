package org.adsl.server.db;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for DatabaseManager.
 * Require a running MySQL instance configured in DatabaseConfig.
 * Run manually or in a CI environment with a database service.
 */
@Disabled("Requires a live MySQL database — run manually")
public class DatabaseManagerTest {

    @Test
    void initDatabase_createsTablesWithoutError() {
        // Verifies that initDatabase() completes without throwing
        DatabaseManager.initDatabase();
    }

    @Test
    void initDatabase_isIdempotent() {
        // Calling twice should not throw (CREATE TABLE IF NOT EXISTS)
        DatabaseManager.initDatabase();
        DatabaseManager.initDatabase();
    }
}
