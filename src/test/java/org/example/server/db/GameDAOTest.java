package org.example.server.db;

import org.example.shared.model.MatchResult;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for MatchResultDAO.
 * Require a running MySQL instance with the schema initialized via DatabaseManager.
 * Run manually or in a CI environment with a database service.
 */
@Disabled("Requires a live MySQL database — run manually")
public class GameDAOTest {

    @Test
    void saveMatch_doesNotThrow() throws SQLException {
        DatabaseManager.initDatabase();
        GameDAO.saveMatch(
                2,
                List.of("Alice", "Bob"),
                List.of(30, 20)
        );
    }

    @Test
    void getLeaderboard_returnsResults() throws SQLException {
        DatabaseManager.initDatabase();
        GameDAO.saveMatch(2, List.of("Alice", "Bob"), List.of(30, 20));
        List<MatchResult> leaderboard = GameDAO.getLeaderboard(2);
        assertFalse(leaderboard.isEmpty());
    }

    @Test
    void getRank_returnsPositiveRank() throws SQLException {
        DatabaseManager.initDatabase();
        GameDAO.saveMatch(2, List.of("Alice", "Bob"), List.of(30, 20));
        int rank = GameDAO.getRank(25, 2);
        assertTrue(rank >= 1);
    }
}
