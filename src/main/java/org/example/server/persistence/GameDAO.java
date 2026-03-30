package org.example.server.persistence;

import org.example.shared.model.MatchResult;

import java.util.List;

public interface GameDAO {
    int createMatch() throws Exception;
    void deleteMatch(int gameId) throws Exception;
    void saveMatch(int gameId, int playerCount,
                          List<String> nicknames,
                          List<Integer> scores) throws Exception;
    List<MatchResult> getLeaderboard(int playerCount) throws Exception;
}
