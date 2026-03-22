package org.example.controller.state;

import org.example.controller.state.utils.Move;
import org.example.exception.InvalidMoveException;
import org.example.model.game.Game;
import org.example.model.game.Player;

import java.util.Set;


public abstract class State {

    private final Game game;

    public State(Game game) {
        this.game = game;
    }

    public State transition(Set<Move> moves, Player p) throws InvalidMoveException {
        checkMove(moves, p);
        execute(moves, p);
        return nextState();
    }

    public State onEntry(){
        return this;
    }
    // NEED A METHOD TO RETURN VALID ACTION FOR EACH PLAYER IN THE CURRENT STATE
    public abstract void checkMove(Set<Move> moves, Player p) throws  InvalidMoveException;
    public abstract void execute(Set<Move> moves, Player p);
    public abstract State nextState();

    public Game getGame() {
        return game;
    }
}
