package org.adsl.server.db;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

/**
 * Integration tests for SqlGameDAO.
 * Require a running MySQL instance with the schema initialized via DatabaseManager.
 * SqlGameDAO is instantiated via ConnectionProvider — run manually against a live DB.
 */
@Disabled("Requires a live MySQL database — run manually")
public class SqlGameDAOTest {

    @Test
    void saveMatch_doesNotThrow() throws SQLException {
        // SqlGameDAO requires a ConnectionProvider instance (not static).
        // To run: instantiate SqlGameDAO with DatabaseConfig::getConnection wrapper and call saveMatch(gameId, playerCount, nicknames, scores).
    }

    @Test
    void getLeaderboard_returnsResults() throws SQLException {
        // To run: instantiate SqlGameDAO and call dao.getLeaderboard(playerCount).
    }

    @Test
    void getRank_returnsPositiveRank() throws SQLException {
        // getRank is not implemented in SqlGameDAO — to be added if needed.
    }
}
