package org.example.controller.state;

import org.example.model.game.Game;

public class EventsState extends State {

    //
    public EventsState(Game game) {
        super(game);
    }

    @Override
    public State transition(Move move) {
        return null;
    }
}
