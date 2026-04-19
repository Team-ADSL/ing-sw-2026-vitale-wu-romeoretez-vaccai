package org.adsl.client.local;

import org.adsl.server.persistence.GameDAO;
import org.adsl.shared.model.MatchResult;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory GameDAO for local mode. No database required.
 */
public class NoOpGameDAO implements GameDAO {
    private final AtomicInteger idCounter = new AtomicInteger(1);
    private volatile int lastCreatedId = -1;

    @Override
    public int createMatch() throws SQLException {
        int id = idCounter.getAndIncrement();
        lastCreatedId = id;
        return id;
    }

    @Override
    public void deleteMatch(int gameId) throws SQLException {
        // No-op
    }

    @Override
    public void saveMatch(int gameId, int playerCount, List<String> nicknames, List<Integer> scores) throws SQLException {
        // No-op: local games are not persisted to any database
    }

    @Override
    public List<MatchResult> getLeaderboard(int playerCount) throws SQLException {
        return new ArrayList<>();
    }

    public int getLastCreatedId() {
        return lastCreatedId;
    }
}
