package org.adsl.server.model;

import java.util.List;

public interface GameObserver {
    void updateGame(Game game);
    void updateLobby(List<String> players, int numPlayersAllowed);
}