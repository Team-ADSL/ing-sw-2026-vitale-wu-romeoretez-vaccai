package org.example.model.state;

import org.example.model.game.Action;
import org.example.model.game.Game;
import org.example.model.game.Player;

public abstract class ModelState {

    private Player activePlayer;
    private Game game;

    public ModelState(Player activePlayer, Game game) {
        this.activePlayer= activePlayer;
        this.game = game;
    }

    public abstract ModelState transition(Action action);

}
