package org.example.server.persistence;

import org.example.shared.model.MatchResult;

import java.sql.SQLException;
import java.util.List;

public interface GameDAO {
    int createMatch() throws SQLException;
    void deleteMatch(int gameId) throws SQLException;
    void saveMatch(int gameId, int playerCount,
                          List<String> nicknames,
                          List<Integer> scores) throws SQLException;
    List<MatchResult> getLeaderboard(int playerCount) throws SQLException;
}
