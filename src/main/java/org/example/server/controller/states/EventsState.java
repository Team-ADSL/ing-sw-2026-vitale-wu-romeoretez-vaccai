package org.example.server.controller.states;

import org.example.server.controller.GameController;
import org.example.shared.enums.Phase;
import org.example.server.model.cards.Card;
import org.example.shared.enums.Trigger;
import org.example.server.model.Game;
import org.example.server.model.Player;

import java.util.Set;

public class EventsState extends ControllerState {

    public EventsState(Game game, GameController context) {
        super(game, context);
    }

    @Override
    public ControllerState onEntry(){
        getGame().setPhase(Phase.EVENTS_EXECUTION);
        Set<Player> players = getGame().getPlayers();
        Card[] cards = getGame().getBoard().getLowRow().getTribeCards();
        for(Card c : cards){
            if(c != null){
                c.activeEffect(players, Trigger.EVENT_EXECUTION);
            }
        }
        getGame().sendUpdateGame();
        return nextState();
    }

    @Override
    public ControllerState nextState() {
        return getGame().getRound() == 10 ?
                new EndRoundState(getGame(), getContext()) : new EndGameState(getGame(), getContext());
    }
}
