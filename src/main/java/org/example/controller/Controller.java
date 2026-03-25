package org.example.controller;

import org.example.controller.state.State;
import org.example.controller.utils.Move;
import org.example.model.game.Player;

import java.util.Set;

public class Controller {
    private final int gameId;
    private State state;

    public Controller(int gameId, State state) {
        this.gameId = gameId;
        this.state = state;
    }

    public synchronized void handleMoveRequest(Set<Move> moves, Player p){
        State newState = state.transition(moves, p);
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
}
