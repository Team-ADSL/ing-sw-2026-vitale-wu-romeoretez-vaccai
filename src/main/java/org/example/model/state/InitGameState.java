package org.example.model.state;

import org.example.model.game.Action;
import org.example.model.game.Game;
import org.example.model.game.Player;

public class InitGameState extends ModelState {

    //

    public InitGameState(Player activePlayer, Game game) {
        super(activePlayer, game);
    }

    @Override
    public ModelState transition(Action action) {
        return null;
    }

}
