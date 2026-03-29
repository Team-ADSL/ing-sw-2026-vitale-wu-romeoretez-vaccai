package org.example.server.controller;

import org.example.server.controller.states.ControllerState;
import org.example.server.network.VirtualClient;
import org.example.shared.exceptions.InvalidRequestException;
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
            state.getGame().updateAll();
            ControllerState newState = state.nextState();
            if(newState != null){
                changeState(newState);
            } else {
                // Save game and remove from lobby
            }
        } catch(InvalidRequestException e){
            System.out.println(e.getMessage());
            virtualClient.sendErrorMessage(e.getMessage());
        }
    }

    // To handle automatics states
    private void changeState(ControllerState newControllerState) {
        ControllerState nextState = newControllerState;
        while (nextState != state) {
            state = nextState;
            nextState = state.onEntry();
            state.getGame().updateAll();
        }
    }

    public ControllerState getState() {
        return state;
    }
}
