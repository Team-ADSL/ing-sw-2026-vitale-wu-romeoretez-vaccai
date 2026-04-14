package org.adsl.server.model;

import org.adsl.shared.model.MatchResult;

import java.util.List;

public interface EndGameObserver {
    void notifyEndGame(int id, List<MatchResult> results);
}
