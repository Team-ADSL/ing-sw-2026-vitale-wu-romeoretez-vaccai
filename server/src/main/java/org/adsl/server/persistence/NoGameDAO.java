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

    @Override
    public int createMatch() throws SQLException {
        return actualCounter;
    }

    @Override
    public void deleteMatch(int gameId) throws SQLException {}

    @Override
    public void saveMatch(int gameId, int playerCount, List<String> nicknames, List<Integer> scores) throws SQLException {}

    @Override
    public List<DBRecord> getLeaderboard(int playerCount) throws SQLException {
        return List.of();
    }

    @Override
    public void setInitialCounter(int i) {
        actualCounter = i + 1;
    }
}
