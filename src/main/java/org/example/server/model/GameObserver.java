package org.example.server.model;

import org.example.shared.model.GameDTO;

import java.util.List;

public interface GameObserver {
    void updateGame(GameDTO game);
    void updateLobby(List<String> players);
}