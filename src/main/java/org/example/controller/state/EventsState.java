package org.example.controller.state;

import org.example.model.game.Action;
import org.example.model.game.Game;
import org.example.model.game.Player;

public class EventsState extends State {

    //
    public EventsState(Player activePlayer) {
        super(activePlayer);
    }

    @Override
    public void notifyClients(Game game) {
    }

    @Override
    public boolean checkInput(Move move, Game game) {
        return false;
    }

    @Override
    public State transition(Move move, Game game) {
        return null;
    }

    public void doEvents(){

    }
}
