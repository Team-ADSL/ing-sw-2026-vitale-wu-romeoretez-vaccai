package org.adsl.server.persistence;

import org.adsl.server.model.Game;
import org.adsl.server.model.GameObserver;

import java.util.List;

/**
 * Strategy interface for crash-recovery persistence of in-progress games.
 * <p>
 * Extends {@link GameObserver} so that implementations can be registered
 * directly as observers and persist state on each {@link #updateGame} call.
 * The production implementation is {@link SerialGamePersistenceManager}.
 * </p>
 */
public interface GamePersistenceManager extends GameObserver {

    /**
     * Loads all previously persisted games from the backing store.
     * Called once at server startup by {@code ServerController.recoverGames()}.
     *
     * @return list of recovered {@link Game} objects (may be empty)
     * @throws Exception if reading from the store fails
     */
    List<Game> recoverGames() throws Exception;

    /**
     * Removes the persisted snapshot for the given game ID, typically after
     * the game has ended or been cancelled.
     *
     * @param gameId the game to remove
     * @throws Exception if the removal fails
     */
    void removeGame(int gameId) throws Exception;
}
