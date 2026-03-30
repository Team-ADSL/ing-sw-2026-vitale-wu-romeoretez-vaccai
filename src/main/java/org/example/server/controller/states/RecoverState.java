package org.example.server.controller.states;

import org.example.server.controller.GameController;
import org.example.server.model.Game;
import org.example.shared.enums.Phase;


public class RecoverState extends ControllerState {
    public RecoverState(Game game, GameController context) {
        super(game, context);
    }

    @Override
    public ControllerState onEntry() {
        getGame().setPhase(Phase.RECOVER);
        return null;
    }


    @Override
    public ControllerState nextState() {
        return null;
    }
}
