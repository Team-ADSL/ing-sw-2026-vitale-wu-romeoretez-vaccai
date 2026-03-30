package org.example.server.model;

import org.example.shared.model.MatchResult;

import java.util.List;

public interface EndGameObserver {
    void update(int gameId, List<MatchResult> matchResults);
}
