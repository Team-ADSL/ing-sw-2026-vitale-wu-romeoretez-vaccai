package org.example.server.controller.states;

import org.example.shared.utils.Move;
import org.example.shared.exceptions.InvalidMoveException;
import org.example.server.model.cards.Card;
import org.example.shared.enums.Trigger;
import org.example.server.model.Game;
import org.example.server.model.Player;

import java.util.Set;

public class EventsControllerState extends ControllerState {

    public EventsControllerState(Game game) {
        super(game);
    }

    @Override
    public ControllerState onEntry(){
        Set<Player> players = getGame().getPlayers();
        Card[] cards = getGame().getBoard().getLowRow().getTribeCards();
        for(Card c : cards){
            if(c != null){
                c.activeEffect(players, Trigger.EVENT_EXECUTION);
            }
        }
        return nextState();
    }

    @Override
    public void checkMove(Set<Move> moves, Player p) throws InvalidMoveException {
        throw new InvalidMoveException("Automatic state: Event execution. No action allowed");
    }

    @Override
    public void execute(Set<Move> moves, Player p) {}

    @Override
    public ControllerState nextState() {
        return getGame().getRound() == 10 ? new EndRoundControllerState(getGame()) : new EndGameControllerState(getGame());
    }

}
