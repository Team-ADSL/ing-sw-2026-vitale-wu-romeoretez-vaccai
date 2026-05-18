package org.adsl.server.db;

import org.adsl.server.persistence.SqlGameDAO;
import org.adsl.shared.model.MatchResult;
import org.junit.jupiter.api.*;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class SqlGameDAOTest {

    private Connection conn;
    private SqlGameDAO dao;

    @BeforeAll
    static void setupDatabase() {
        assumeTrue(System.getenv("DB_PASSWORD") != null, "DB_PASSWORD not set — skipping DB tests");
        DatabaseManager.initDatabase();
    }

    @BeforeEach
    void beginTransaction() throws SQLException {
        conn = DatabaseConfig.getConnection();
        conn.setAutoCommit(false);
        Connection proxy = (Connection) Proxy.newProxyInstance(
            Connection.class.getClassLoader(),
            new Class[]{Connection.class},
            (p, method, args) -> {
                String name = method.getName();
                if ("close".equals(name) || "setAutoCommit".equals(name) || "commit".equals(name))
                    return null;
                return method.invoke(conn, args);
            }
        );
        dao = new SqlGameDAO(() -> proxy);
    }

    @AfterEach
    void rollbackTransaction() throws SQLException {
        if (conn != null) {
            conn.rollback();
            conn.close();
        }
    }

    @Test
    void testCreateMatch_returnsPositiveId() throws SQLException {
        int id = dao.createMatch();
        assertTrue(id > 0);
    }

    @Test
    void testCreateMatch_returnsUniqueIds() throws SQLException {
        int id1 = dao.createMatch();
        int id2 = dao.createMatch();
        assertNotEquals(id1, id2);
    }

    @Test
    void testDeleteMatch_removesExistingMatch() throws SQLException {
        int id = dao.createMatch();
        assertDoesNotThrow(() -> dao.deleteMatch(id));
    }

    @Test
    void testDeleteMatch_throwsForNonExistentMatch() {
        assertThrows(SQLException.class, () -> dao.deleteMatch(Integer.MAX_VALUE));
    }

    @Test
    void testSaveMatch_doesNotThrow() throws SQLException {
        int id = dao.createMatch();
        assertDoesNotThrow(() -> dao.saveMatch(id, 2, List.of("Alice", "Bob"), List.of(100, 80)));
    }

    @Test
    void testSaveMatch_persistsCorrectNumberOfResults() throws SQLException {
        int id = dao.createMatch();
        dao.saveMatch(id, 2, List.of("Alice", "Bob"), List.of(100, 80));

        List<MatchResult> leaderboard = dao.getLeaderboard(2);
        assertEquals(2, leaderboard.size());
    }

    @Test
    void testGetLeaderboard_orderedByScoreDescending() throws SQLException {
        int id = dao.createMatch();
        dao.saveMatch(id, 2, List.of("Alice", "Bob"), List.of(80, 100));

        List<MatchResult> leaderboard = dao.getLeaderboard(2);
        assertEquals("Bob", leaderboard.get(0).nickname());
        assertEquals("Alice", leaderboard.get(1).nickname());
    }

    @Test
    void testGetLeaderboard_rankAssignedCorrectly() throws SQLException {
        int id = dao.createMatch();
        dao.saveMatch(id, 2, List.of("Alice", "Bob"), List.of(80, 100));

        List<MatchResult> leaderboard = dao.getLeaderboard(2);
        assertEquals(1, leaderboard.get(0).rank());
        assertEquals(2, leaderboard.get(1).rank());
    }

    @Test
    void testGetLeaderboard_tieSharesSameRank() throws SQLException {
        int id = dao.createMatch();
        dao.saveMatch(id, 2, List.of("Alice", "Bob"), List.of(100, 100));

        List<MatchResult> leaderboard = dao.getLeaderboard(2);
        assertEquals(leaderboard.get(0).rank(), leaderboard.get(1).rank());
    }

    @Test
    void testGetLeaderboard_emptyForUnknownPlayerCount() throws SQLException {
        List<MatchResult> leaderboard = dao.getLeaderboard(99);
        assertTrue(leaderboard.isEmpty());
    }

    @Test
    void testSaveMatch_samePlayerMultipleMatches_accumulatesScore() throws SQLException {
        int id1 = dao.createMatch();
        dao.saveMatch(id1, 2, List.of("Alice", "Bob"), List.of(100, 80));
        int id2 = dao.createMatch();
        dao.saveMatch(id2, 2, List.of("Alice", "Bob"), List.of(50, 120));

        List<MatchResult> leaderboard = dao.getLeaderboard(2);
        assertEquals("Bob", leaderboard.get(0).nickname());
        assertEquals(200, leaderboard.get(0).score());
        assertEquals("Alice", leaderboard.get(1).nickname());
        assertEquals(150, leaderboard.get(1).score());
    }

    @Test
    void testSaveMatch_playerCountFilterIsolatesResults() throws SQLException {
        int id2p = dao.createMatch();
        dao.saveMatch(id2p, 2, List.of("Alice", "Bob"), List.of(100, 80));
        int id3p = dao.createMatch();
        dao.saveMatch(id3p, 3, List.of("Carol", "Dave", "Eve"), List.of(50, 60, 70));

        List<MatchResult> leaderboard2 = dao.getLeaderboard(2);
        List<MatchResult> leaderboard3 = dao.getLeaderboard(3);

        assertEquals(2, leaderboard2.size());
        assertEquals(3, leaderboard3.size());
    }

    @Test
    void testSaveMatch_throwsSQLExceptionForNonExistentMatch() {
        assertThrows(SQLException.class,
                () -> dao.saveMatch(Integer.MAX_VALUE, 2, List.of("Alice", "Bob"), List.of(100, 80)));
    }

    @Test
    void testGetLeaderboard_rankGapAfterTie() throws SQLException {
        int id = dao.createMatch();
        dao.saveMatch(id, 3, List.of("Alice", "Bob", "Carol"), List.of(100, 100, 80));

        List<MatchResult> leaderboard = dao.getLeaderboard(3);
        assertEquals(1, leaderboard.get(0).rank());
        assertEquals(1, leaderboard.get(1).rank());
        assertEquals(3, leaderboard.get(2).rank());
    }
}
