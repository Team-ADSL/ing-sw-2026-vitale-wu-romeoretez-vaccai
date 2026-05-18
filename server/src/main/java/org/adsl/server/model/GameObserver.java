package org.adsl.server.model;

import org.adsl.shared.enums.Totem;

import java.util.List;

/**
 * Observer for in-game state changes. Implemented by {@code VirtualClient} to
 * push updates to connected clients and by {@code GamePersistenceManager} to
 * persist state on each change.
 */
public interface GameObserver {

    /**
     * Called when the game model has changed and all clients should receive a
     * fresh {@code GameDTO} snapshot.
     *
     * @param game the updated game model
     */
    void updateGame(Game game);

    default void updateGame(Game game, String message) {
        updateGame(game);
    }

    /**
     * Called when the lobby roster changes (player joined or left).
     *
     * @param gameId            the game identifier
     * @param players           current list of player names in the lobby
     * @param numPlayersAllowed maximum players for this game
     */
    void updateLobby(int gameId, List<String> players, int numPlayersAllowed);

    /**
     * Called during totem-picking to broadcast the list of totems still
     * available for selection.
     *
     * @param totemsAvailable totems not yet chosen by any player
     * @param message         log message to accompany the update
     */
    void updateTotemAvailable(List<Totem> totemsAvailable, String message);

    default void updateLobby(int gameId, List<String> players, int numPlayersAllowed, String message) {
        updateLobby(gameId, players, numPlayersAllowed);
    }

    default void notifyError(String message) {}

    default void notifyEventTriggered(String eventTitle, String logMessage) {}
}
