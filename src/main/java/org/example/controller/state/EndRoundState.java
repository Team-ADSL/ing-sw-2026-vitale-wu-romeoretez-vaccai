package org.example.controller.state;

import org.example.controller.utils.Move;
import org.example.exception.InvalidMoveException;
import org.example.model.card.Card;
import org.example.model.game.Game;
import org.example.model.game.Player;
import org.example.model.game.boardComponent.CardRow;
import org.example.model.game.boardComponent.Deck;

import java.util.Set;

public class EndRoundState extends State {

    public EndRoundState(Game game) {
        super(game);
    }

    @Override
    public State onEntry(){
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
    public State nextState() {
        return new TotemPlacementState(getGame(), 0);
    }


}
