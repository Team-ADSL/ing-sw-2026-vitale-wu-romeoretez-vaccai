package org.example.server.model;

import org.example.shared.model.MatchResult;

import java.sql.SQLException;
import java.util.List;

public interface EndGameData {
    void notifyEndGame(List<MatchResult> matchResults) throws SQLException;
    void addObserver(EndGameObserver o);
    void removeObserver(EndGameObserver o);
}
