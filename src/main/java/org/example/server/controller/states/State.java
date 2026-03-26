package org.example.server.controller.states;

import org.example.shared.utils.Move;
import org.example.shared.exceptions.InvalidMoveException;
import org.example.server.model.Game;
import org.example.server.model.Player;

import java.util.Set;


public abstract class State {

    private final Game game;

    public State(Game game) {
        this.game = game;
    }

    public State transition(Set<Move> moves, Player p) {
        try {
            checkMove(moves, p);
            execute(moves, p);
            return nextState();
        } catch (InvalidMoveException e) {
            getGame().updateAll(e.getMessage());
            return this;
        }
    }

    public State onEntry(){
        return this;
    }
    public abstract void checkMove(Set<Move> moves, Player p) throws  InvalidMoveException;
    public abstract void execute(Set<Move> moves, Player p);
    public abstract State nextState();

    public Game getGame() {
        return game;
    }
}
