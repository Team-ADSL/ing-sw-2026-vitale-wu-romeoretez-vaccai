package org.example.controller.state;

import org.example.exception.InvalidMoveException;
import org.example.model.game.Game;

import java.util.Set;


// NOTA: ACTIVE PLAYER NON E NECESSARIO, SISTEMA TUTTI GLI STATI
// BASTA LA CASELLA ATTIVA (se serve)

public abstract class State {

    private Game game;

    public State(Game game) {
        this.game = game;
    }

    public abstract State transition(Set<Move> moves) throws InvalidMoveException;

    public Game getGame() {
        return game;
    }
}
