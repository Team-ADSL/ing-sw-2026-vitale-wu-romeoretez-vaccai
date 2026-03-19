package org.example.controller.state;

import org.example.model.game.Game;


// NOTA: ACTIVE PLAYER NON E NECESSARIO, SISTEMA TUTTI GLI STATI
// BASTA LA CASELLA ATTIVA (se serve)

// SOSTITUISCI PACKAGE CON SEMPLICI FOLDER (MODULE)

public abstract class State {

    private Game game;

    public State(Game game) {
        this.game = game;
    }

    // SET DI MOSSE in entrata
    // LANCIA EXCEPTION PER INVALID INPUT
    public abstract State transition(Move move);

    public Game getGame() {
        return game;
    }
}
