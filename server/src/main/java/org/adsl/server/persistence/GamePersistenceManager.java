package org.adsl.server.persistence;

import org.adsl.server.model.Game;
import org.adsl.server.model.GameObserver;

import java.util.List;

public interface GamePersistenceManager extends GameObserver {
    List<Game> recoverGames() throws Exception;
    void removeGame(int gameId) throws Exception;
}
