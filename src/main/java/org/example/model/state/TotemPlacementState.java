package org.example.model.state;

import org.example.model.game.Action;
import org.example.model.game.Game;
import org.example.model.game.Player;

public class TotemPlacementState extends ModelState {

    private int orderIndex;

    public TotemPlacementState(Player activePlayer, Game game, int orderIndex) {
        super(activePlayer, game);
        this.orderIndex = orderIndex;
    }

    @Override
    public ModelState transition(Action action) {
        return null;
    }
}
