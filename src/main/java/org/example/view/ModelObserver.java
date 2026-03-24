package org.example.view;

import org.example.model.game.Game;

public interface ModelObserver {
    void update(Game game, String error);
}
