package org.adsl.fakes;

import org.adsl.server.model.Game;
import org.adsl.server.persistence.GamePersistenceManager;

import java.util.ArrayList;
import java.util.List;

public class FakeGamePersistenceManager implements GamePersistenceManager {
    public final List<Integer> removedGames = new ArrayList<>();

    @Override public void removeGame(int id) { removedGames.add(id); }
    @Override public List<Game> recoverGames() { return new ArrayList<>(); }
    @Override public void updateGame(Game game) {}
    @Override public void updateLobby(List<String> players) {}
}
