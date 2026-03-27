package org.example.server.controller;

import org.example.server.controller.states.State;
import org.example.shared.utils.Move;
import org.example.server.model.Player;

import java.util.Set;

public class GameController {
    private final int gameId;
    private State state;

    public GameController(int gameId, State state) {
        this.gameId = gameId;
        this.state = state;
    }

    public synchronized void handleRequest(Set<Move> moves, String username){
        State newState = state.transition(moves, username);
        state.getGame().updateAll(null);
        changeState(newState);
    }

    // To handle automatics states
    private void changeState(State newState) {
        State nextState = newState;
        while (nextState != state) {
            state = nextState;
            nextState = state.onEntry();
            state.getGame().updateAll(null);
        }
    }

    public State getState() {
        return state;
    }
}
