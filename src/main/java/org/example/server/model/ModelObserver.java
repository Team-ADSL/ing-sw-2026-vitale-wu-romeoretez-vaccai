package org.example.server.model;

import org.example.shared.model.GameDTO;

import java.util.List;

public interface ModelObserver {
    void updateHome(List<Integer> activeGames);
    void updateLobby(List<String> players);
    void updateGame(GameDTO game);
}
