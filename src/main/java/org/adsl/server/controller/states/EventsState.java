package org.adsl.server.controller.states;

import org.adsl.server.controller.GameController;
import org.adsl.shared.enums.Phase;
import org.adsl.server.model.cards.Card;
import org.adsl.shared.enums.Trigger;
import org.adsl.server.model.Game;
import org.adsl.server.model.Player;

import java.util.ArrayList;
import java.util.Set;

public class EventsState extends ControllerState {

    public EventsState(Game game, GameController context) {
        super(game, context);
    }

    @Override
    public ControllerState onEntry(){
        Set<Player> players = getGame().getPlayers();
        ArrayList<Card> cards = getGame().getBoard().lowRow().getTribeCards();
        for(Card c : cards){
            if(c != null){
                c.activeEffect(players, Trigger.EVENT_EXECUTION);
            }
        }
        setNextState(calcNextState());
        getGame().sendUpdateGame();
        return getNextState();
    }

    @Override
    public ControllerState calcNextState() {
        if(getGame().getRound() != 10){
            getGame().setPhase(Phase.END_ROUND);
            return new EndRoundState(getGame(), getContext());
        } else {
            getGame().setPhase(Phase.END_GAME);
            return new EndGameState(getGame(), getContext());
        }
    }
}
