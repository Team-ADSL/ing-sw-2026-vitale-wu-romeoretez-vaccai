package org.example.controller.state;

import org.example.controller.state.utils.Move;
import org.example.model.game.Game;

public class EndRoundState extends State {

    public EndRoundState(Game game) {
        super(game);
    }


    @Override
    public State transition(Move move) {
        return null;
    }

    public void changeEra(){

    }
}
