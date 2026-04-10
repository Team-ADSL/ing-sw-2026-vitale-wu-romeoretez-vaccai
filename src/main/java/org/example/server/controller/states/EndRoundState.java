package org.example.server.controller.states;

import org.example.server.controller.GameController;
import org.example.shared.enums.Phase;
import org.example.server.model.cards.Card;
import org.example.server.model.Game;
import org.example.server.model.board.CardRow;
import org.example.server.model.board.Deck;

import java.util.ArrayList;
import java.util.Set;

public class EndRoundState extends ControllerState {

    public EndRoundState(Game game, GameController context) {
        super(game, context);
    }

    @Override
    public ControllerState onEntry() {
        CardRow lowRow = getGame().getBoard().lowRow();
        CardRow topRow = getGame().getBoard().topRow();

        // Move cards from top to low row
        lowRow.clearTribeCards();
        ArrayList<Card> cardsToMove = topRow.getTribeCards();
        topRow.clearTribeCards();
        lowRow.addTribeCards(cardsToMove);

        // Re-fill top row
        Deck deck = getGame().getBoard().deck();
        for(int i=0; i < topRow.getNumTribeCard(); i++){
            Card newCard = deck.drawCard();
            topRow.add(newCard);
        }

        // Handle new era
        if(deck.isNewEra(getGame().getEra())){
            getGame().changeEra();

            if(getGame().getEra() == 3){
                lowRow.clearBuildings();
            }

            // Move top buildings
            Set<Card> buildingToMove = topRow.getBuildings();
            topRow.clearBuildings();
            lowRow.addBuildings(buildingToMove);

            Set<Card> newBuildings = getGame().getBoard().remainingBuildings().removeFirst();
            topRow.addBuildings(newBuildings);
        }
        getGame().changeRound();
        setNextState(calcNextState());
        getGame().sendUpdateGame();
        return getNextState();
    }

    @Override
    public ControllerState calcNextState() {
        getGame().setPhase(Phase.TOTEM_PLACEMENT);
        return new TotemPlacementState(getGame(), getContext());
    }
}
