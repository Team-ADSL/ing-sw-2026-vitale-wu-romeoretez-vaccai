package org.example.server.controller.states;

import org.example.server.controller.GameController;
import org.example.server.model.Game;
import org.example.shared.enums.Phase;


public class RecoverState extends ControllerState {
    private final int gameId;

    public RecoverState(Game game, GameController context, int gameId) {
        super(game, context);
        this.gameId = gameId;
    }

    @Override
    public ControllerState onEntry() {
        getGame().setPhase(Phase.RECOVER);
        // Recover logic
        getGame().sendUpdateGame();
        return null;
    }


    @Override
    public ControllerState nextState() {
        return null;
    }
}
