package org.adsl.server.persistence;

import org.adsl.shared.model.MatchResult;

import java.sql.SQLException;
import java.util.List;

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
    public List<MatchResult> getLeaderboard(int playerCount) throws SQLException {
        return List.of();
    }

    @Override
    public void setInitialCounter(int i) {
        actualCounter = i + 1;
    }
}
