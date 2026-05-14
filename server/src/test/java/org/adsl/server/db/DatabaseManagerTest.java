package org.adsl.server.db;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled("Requires a live MySQL database — run manually")
public class DatabaseManagerTest {

    @Test
    void testInitDatabase_createsTablesWithoutError() {
        DatabaseManager.initDatabase();
    }

    @Test
    void testInitDatabase_isIdempotent() {
        DatabaseManager.initDatabase();
        DatabaseManager.initDatabase();
    }
}
