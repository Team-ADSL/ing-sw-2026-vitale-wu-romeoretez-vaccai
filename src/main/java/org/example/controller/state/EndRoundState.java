package org.example.controller.state;

import org.example.controller.state.utils.Move;
import org.example.exception.InvalidMoveException;
import org.example.model.card.Card;
import org.example.model.card.building.Building;
import org.example.model.game.Game;
import org.example.model.game.Player;
import org.example.model.game.boardComponent.CardRow;
import org.example.model.game.boardComponent.Deck;

import java.util.Arrays;
import java.util.Set;

public class EndRoundState extends State {

    public EndRoundState(Game game) {
        super(game);
    }

    @Override
    public State onEntry(){
        CardRow lowRow = getGame().getBoard().getLowRow();
        CardRow topRow = getGame().getBoard().getTopRow();

        getGame().getBoard().getLowRow().clear();

        Card[] cardsTopRow = topRow.getCards();
        Card[] cardsToMove = Arrays.copyOf(cardsTopRow, topRow.getNonBuildingCard());
        lowRow.addCards(cardsToMove);

        Deck deck = getGame().getBoard().getDeck();
        for(int i=0; i < topRow.getNonBuildingCard(); i++){
            Card newCard = deck.drawCard();
            topRow.add(newCard);
        }

        if(deck.isNewEra(getGame().getEra())){
            getGame().changeEra();
            if(getGame().getEra() != 3){
                Set<Card> buildingToMove = topRow.extractBuilding();
                lowRow.addCardsFrom(buildingToMove, lowRow.getNonBuildingCard());

                Set<Card> newBuildings = getGame().getBoard().getRemainingBuildings().removeFirst();
                topRow.addCardsFrom(newBuildings, topRow.getNonBuildingCard());
            } else {
                
            }
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
