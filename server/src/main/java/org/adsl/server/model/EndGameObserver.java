package org.adsl.server.model;

import org.adsl.shared.model.DBRecord;
import org.adsl.shared.model.MatchResult;

import java.util.List;

/**
 * Observer notified when a game ends, either normally (with leaderboard data)
 * or prematurely (all players left the lobby before starting, in which case
 * {@code results} is {@code null}).
 */
public interface EndGameObserver {

    /**
     * Called when game {@code id} has finished.
     *
     * @param id      the game identifier
     * @param results leaderboard results, or {@code null} if the game never started
     */
    void notifyEndGame(int id, List<MatchResult> results,List<DBRecord> records);

    /**
     * Called when game {@code id} has finished with an optional log message.
     * Default implementation delegates to {@link #notifyEndGame(int, List, List)}.
     *
     * @param id      the game identifier
     * @param results leaderboard results, or {@code null} if the game never started
     * @param message optional message to display in the game log
     */
    default void notifyEndGame(int id, List<MatchResult> results, List<DBRecord> records, String message) {
        notifyEndGame(id, results, records);
    }
}
