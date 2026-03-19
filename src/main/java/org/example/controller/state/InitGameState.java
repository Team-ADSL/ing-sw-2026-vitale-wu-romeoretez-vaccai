package org.example.controller.state;

import org.example.model.game.Game;

public class InitGameState extends State {

    public InitGameState(Game game) {
        super(game);
    }

    @Override
    public State transition(Move move) {
        return null;
    }
}
