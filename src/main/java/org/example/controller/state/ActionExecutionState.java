package org.example.controller.state;

import org.example.model.card.Card;
import org.example.model.game.Game;
import org.example.model.game.Player;

import java.util.ArrayList;


public class ActionExecutionState extends State {

    private int offerIndex;
    private int remainingMoves;

    public ActionExecutionState(Game game, int offerIndex, int remainingMoves) {
        super(game);
        this.offerIndex = offerIndex;
        this.remainingMoves = remainingMoves;
    }

    @Override
    public State transition(Move move) {
        ArrayList<Card> selectedRow = null;
        if(move.getRow() == Row.UPPER) {
            selectedRow = game.getBoard().getTopRow();
        } else {
            selectedRow = game.getBoard().getLowRow();
        }
        Card selectedCard = selectedRow.get(move.getRowIndex());
        selectedCard.insert(getActivePlayer().getCards());
        return null;
    }
}