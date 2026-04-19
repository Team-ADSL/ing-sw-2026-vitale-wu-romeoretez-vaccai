package org.adsl.client.local;

import org.adsl.server.model.Game;
import org.adsl.server.persistence.GamePersistenceManager;

import java.util.ArrayList;
import java.util.List;

/**
 * No-op persistence manager for local mode. Games are not saved to disk.
 */
public class NoOpGamePersistenceManager implements GamePersistenceManager {

    @Override
    public List<Game> recoverGames() throws Exception {
        return new ArrayList<>();
    }

    @Override
    public void removeGame(int gameId) throws Exception {
        // No-op
    }

    @Override
    public void updateGame(Game game) {
        // No-op: local games do not need crash recovery
    }

    @Override
    public void updateLobby(List<String> players) {
        // No-op
    }
}
