package org.example.controller.state;

import org.example.model.game.Game;


// NOTA: ACTIVE PLAYER NON E NECESSARIO, SISTEMA TUTTI GLI STATI
// BASTA LA CASELLA ATTIVA (se serve)
public abstract class State {

    public State() {

    }

    public abstract void notifyClients(Game game);
    public abstract boolean checkInput(Move move, Game game);
    public abstract State transition(Move move, Game game);

}
