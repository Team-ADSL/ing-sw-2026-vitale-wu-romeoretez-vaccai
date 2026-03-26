package org.example.server.model;

import org.example.shared.model.GameDTO;

public interface ModelObserver {
    void update(GameDTO game, String error);
}
