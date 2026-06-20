package org.adsl.server.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class NoGameDAOTest {

    private NoGameDAO dao;

    @BeforeEach
    void setUp() {
        dao = new NoGameDAO();
    }

    // ──────────────────────────────────────────────
    // createMatch
    // ──────────────────────────────────────────────

    @Test
    void testCreateMatch_withoutSetInitialCounter_returnsZero() throws SQLException {
        assertEquals(0, dao.createMatch());
    }

    @Test
    void testCreateMatch_calledTwice_returnDifferentCounterSecondTime() throws SQLException {
        int first = dao.createMatch();
        int second = dao.createMatch();
        assertEquals(first + 1, second, "NoGameDAO counter is not incremented — same ID returned each time");
    }

    // ──────────────────────────────────────────────
    // setInitialCounter
    // ──────────────────────────────────────────────

    @Test
    void testSetInitialCounter_setsCounterToPlusOne() throws SQLException {
        dao.setInitialCounter(9);
        assertEquals(10, dao.createMatch());
    }

    @Test
    void testSetInitialCounter_zero_returnsOne() throws SQLException {
        dao.setInitialCounter(0);
        assertEquals(1, dao.createMatch());
    }

    // ──────────────────────────────────────────────
    // deleteMatch
    // ──────────────────────────────────────────────

    @Test
    void testDeleteMatch_doesNotThrow() {
        assertDoesNotThrow(() -> dao.deleteMatch(42));
    }

    // ──────────────────────────────────────────────
    // saveMatch
    // ──────────────────────────────────────────────

    @Test
    void testSaveMatch_doesNotThrow() {
        assertDoesNotThrow(() ->
                dao.saveMatch(1, 2, List.of("Alice", "Bob"), List.of(10, 20)));
    }

    // ──────────────────────────────────────────────
    // getLeaderboard
    // ──────────────────────────────────────────────

    @Test
    void testGetLeaderboard_alwaysReturnsEmptyList() throws SQLException {
        List<?> result = dao.getLeaderboard(2);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetLeaderboard_differentPlayerCounts_alwaysReturnsEmpty() throws SQLException {
        for (int n = 2; n <= 5; n++) {
            assertTrue(dao.getLeaderboard(n).isEmpty(),
                    "Expected empty leaderboard for " + n + " players");
        }
    }
}
