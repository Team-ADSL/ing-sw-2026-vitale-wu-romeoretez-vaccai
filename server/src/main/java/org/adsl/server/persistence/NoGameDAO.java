package org.adsl.server.persistence;

import org.adsl.shared.model.DBRecord;

import java.sql.SQLException;
import java.util.List;

/**
 * No-op {@link GameDAO} used when the server starts without a reachable DBMS.
 * <p>
 * All write operations ({@link #saveMatch}, {@link #deleteMatch}) are silent
 * no-ops. {@link #getLeaderboard} always returns an empty list so
 * {@code EndGameState} can detect the "no DB" condition and log an appropriate
 * message instead of throwing. {@link #createMatch} returns a counter seeded
 * by {@link #setInitialCounter} so game IDs remain unique within a session even
 * without a database.
 * </p>
 */
public class NoGameDAO implements GameDAO {
    private int actualCounter = 0;

    /**
     * Returns the next session-local match ID without touching any database.
     *
     * @return a counter-based ID, unique within this server session
     * @throws SQLException never thrown by this implementation
     */
    @Override
    public int createMatch() throws SQLException {
        return actualCounter;
    }

    /**
     * No-op: there is no database row to delete.
     *
     * @param gameId the match ID (unused)
     * @throws SQLException never thrown by this implementation
     */
    @Override
    public void deleteMatch(int gameId) throws SQLException {}

    /**
     * No-op: match results are not persisted without a database.
     *
     * @param gameId      the match ID (unused)
     * @param playerCount number of players (unused)
     * @param nicknames   player nicknames (unused)
     * @param scores      final scores (unused)
     * @throws SQLException never thrown by this implementation
     */
    @Override
    public void saveMatch(int gameId, int playerCount, List<String> nicknames, List<Integer> scores) throws SQLException {}

    /**
     * @param playerCount filter for matches with this exact number of players (unused)
     * @return always an empty list, since no leaderboard data is stored
     * @throws SQLException never thrown by this implementation
     */
    @Override
    public List<DBRecord> getLeaderboard(int playerCount) throws SQLException {
        return List.of();
    }

    /**
     * Seeds the in-memory match counter so subsequently created matches get
     * IDs starting from {@code i + 1}, avoiding collisions with games recovered
     * from disk.
     *
     * @param i max index among the recovered games
     */
    @Override
    public void setInitialCounter(int i) {
        actualCounter = i + 1;
    }
}
