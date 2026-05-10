package org.adsl.server.model;

import java.util.List;

public interface GameObserver {
    void updateGame(Game game);

    default void updateGame(Game game, String message) {
        updateGame(game);
    }

    void updateLobby(int gameId, List<String> players, int numPlayersAllowed);

    default void notifyError(String message) {}
}
