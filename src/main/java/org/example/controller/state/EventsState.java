package org.example.controller.state;

import org.example.controller.state.utils.Move;
import org.example.exception.InvalidMoveException;
import org.example.model.card.Card;
import org.example.model.card.Trigger;
import org.example.model.card.building.EndGame;
import org.example.model.game.Game;
import org.example.model.game.Player;

import java.util.Set;

public class EventsState extends State {

    public EventsState(Game game) {
        super(game);
    }

    @Override
    public State onEntry(){
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
    public State nextState() {
        return getGame().getRound() == 10 ? new EndRoundState(getGame()) : new EndGameState(getGame());
    }

}
