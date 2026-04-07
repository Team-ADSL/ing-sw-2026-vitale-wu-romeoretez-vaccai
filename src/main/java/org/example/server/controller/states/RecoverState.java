package org.example.server.controller.states;

import org.example.server.controller.GameController;
import org.example.server.controller.StateFactory;
import org.example.server.model.Game;
import org.example.shared.enums.Phase;


public class RecoverState extends ControllerState {
    public RecoverState(Game game, GameController context) {
        super(game, context);
    }

    @Override
    public ControllerState onEntry() {
        getGame().setPhase(Phase.RECOVER);
        getGame().sendUpdateLobby();
        return this;
    }

    // Implement methods similar to the LobbyState for reconnection

    @Override
    public ControllerState nextState() {
        // ADD CONTROL TO VERIFY IF EVERY PLAYER IS CONNECTED AGAIN
        if(true){
            return StateFactory.recover(getGame(), getContext());
        } else {
            return this;
        }
    }
}
