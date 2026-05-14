package org.adsl.server.model;

import org.adsl.shared.enums.Totem;

import java.util.List;

public interface GameObserver {
    void updateGame(Game game);

    default void updateGame(Game game, String message) {
        updateGame(game);
    }

    void updateLobby(int gameId, List<String> players, int numPlayersAllowed);
    void updateTotemAvailable(List<Totem> totemsAvailable, String message);

    default void updateLobby(int gameId, List<String> players, int numPlayersAllowed, String message) {
        updateLobby(gameId, players, numPlayersAllowed);
    }

    default void notifyError(String message) {}

    default void notifyEventTriggered(String eventTitle, String logMessage) {}
}
