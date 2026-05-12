package org.adsl.utils.fakes;

import org.adsl.server.model.Game;
import org.adsl.server.persistence.GamePersistenceManager;
import org.adsl.shared.enums.Totem;

import java.util.ArrayList;
import java.util.List;

public class FakeGamePersistenceManager implements GamePersistenceManager {
    public final List<Integer> removedGames = new ArrayList<>();

    @Override public void removeGame(int id) { removedGames.add(id); }
    @Override public List<Game> recoverGames() { return new ArrayList<>(); }
    @Override public void updateGame(Game game) {}
    @Override public void updateLobby(int gameId, List<String> players, int numPlayers) {}
    @Override
    public void updateTotemAvailable(List<Totem> totemsAvailable, String message) {

    }
}
