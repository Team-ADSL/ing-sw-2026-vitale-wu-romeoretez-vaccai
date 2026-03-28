package org.example.server.controller;

import org.example.server.controller.states.ControllerState;
import org.example.server.network.VirtualClient;
import org.example.shared.exceptions.InvalidMoveException;
import org.example.shared.network.requests.ClientRequest;


public class GameController {
    private final int gameId;
    private ControllerState state;

    public GameController(int gameId, ControllerState state) {
        this.gameId = gameId;
        this.state = state;
    }

    public synchronized void handleClientRequest(ClientRequest req, VirtualClient virtualClient){
        try {
            req.accept(state, virtualClient);
            state.getGame().updateAll(null);
            ControllerState newState = state.nextState();
            changeState(newState);
        } catch(InvalidMoveException e){
            System.out.println(e.getMessage());
            state.getGame().updateAll(e.getMessage());
        }
    }

    // To handle automatics states
    private void changeState(ControllerState newControllerState) {
        ControllerState nextState = newControllerState;
        while (nextState != state) {
            state = nextState;
            nextState = state.onEntry();
            state.getGame().updateAll(null);
        }
    }

    public ControllerState getState() {
        return state;
    }
}
