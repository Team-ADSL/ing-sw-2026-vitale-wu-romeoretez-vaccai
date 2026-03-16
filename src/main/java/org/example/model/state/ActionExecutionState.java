package org.example.model.state;

import org.example.model.game.Action;
import org.example.model.game.Game;
import org.example.model.game.Player;

public class ActionExecutionState extends ModelState {

    private int offerIndex;

    public ActionExecutionState(Player activePlayer, Game game, int offerIndex) {
        super(activePlayer, game);
        this.offerIndex = offerIndex;
    }

    @Override
    public ModelState transition(Action action) {
        return null;
    }
}
