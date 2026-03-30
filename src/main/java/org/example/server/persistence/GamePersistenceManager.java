package org.example.server.persistence;

import org.example.server.model.Game;
import org.example.server.model.ModelObserver;

import java.util.List;

public interface GamePersistenceManager extends ModelObserver {
    List<Game> recoverGames() throws Exception;
    void removeGame(int gameId) throws Exception;
}
