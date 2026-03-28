package org.example.server.controller.states;

import org.example.shared.utils.Move;
import org.example.shared.exceptions.InvalidMoveException;
import org.example.server.model.cards.Card;
import org.example.server.model.Game;
import org.example.server.model.Player;
import org.example.server.model.board.CardRow;
import org.example.server.model.board.Deck;

import java.util.Set;

public class EndRoundControllerState extends ControllerState {

    public EndRoundControllerState(Game game) {
        super(game);
    }

    @Override
    public ControllerState onEntry(){
        CardRow lowRow = getGame().getBoard().getLowRow();
        CardRow topRow = getGame().getBoard().getTopRow();

        // Move cards from top to low row
        lowRow.clearTribeCards();
        Card[] cardsToMove= topRow.getTribeCards();
        topRow.clearTribeCards();
        lowRow.addTribeCards(cardsToMove);

        // Re-fill top row
        Deck deck = getGame().getBoard().getDeck();
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
            lowRow.addBuildings(buildingToMove);
            topRow.clearBuildings();
            lowRow.addBuildings(buildingToMove);

            Set<Card> newBuildings = getGame().getBoard().getRemainingBuildings().removeFirst();
            topRow.addBuildings(newBuildings);
        }
        return nextState();
    }

    @Override
    public void checkMove(Set<Move> moves, Player p) throws InvalidMoveException {
        throw new InvalidMoveException("Automatic state: EndRound execution. No action allowed");
    }

    @Override
    public void execute(Set<Move> moves, Player p) {}

    @Override
    public ControllerState nextState() {
        return new TotemPlacementControllerState(getGame());
    }


}
