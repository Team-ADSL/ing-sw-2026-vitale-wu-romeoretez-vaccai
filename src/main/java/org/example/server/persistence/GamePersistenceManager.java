package org.example.server.persistence;

import org.example.server.model.Game;
import org.example.server.model.GameObserver;

import java.util.List;

public interface GamePersistenceManager extends GameObserver {
    List<Game> recoverGames() throws Exception;
    void removeGame(int gameId) throws Exception;
}
