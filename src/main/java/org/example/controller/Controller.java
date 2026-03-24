package org.example.controller;

import org.example.controller.state.State;
import org.example.controller.state.utils.Move;
import org.example.exception.InvalidMoveException;
import org.example.model.game.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Controller {
    private final int gameId;
    private State state;
    private final Map<Player, ViewHandler> views;

    public Controller(int gameId, State state) {
        this.gameId = gameId;
        this.state = state;
        this.views = new HashMap<>();
    }

    public Controller(int gameId, State state, Map<Player, ViewHandler> views) {
        this.gameId = gameId;
        this.state = state;
        this.views = views;
    }

    public synchronized void handleMoveRequest(Set<Move> moves, Player p){
        try {
            State newState = state.transition(moves, p);
            updateViews();
            changeState(newState);
        } catch (InvalidMoveException e) {
            views.get(p).showErrorMessage(e.getMessage());
        }
    }

    // To handle automatics states
    private void changeState(State newState) {
        State nextState = newState;
        while (nextState != state) {
            state = nextState;
            nextState = state.onEntry();
            updateViews();
        }
        notifyActionsForPlayers();
    }

    private void notifyActionsForPlayers(){

    }
    private void updateViews(){
        views.values().forEach(v -> v.updateView(state.getGame()));
    }
}
