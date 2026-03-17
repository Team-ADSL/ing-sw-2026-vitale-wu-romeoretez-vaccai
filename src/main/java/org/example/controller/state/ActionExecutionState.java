package org.example.controller.state;

import org.example.model.card.Card;
import org.example.model.game.Game;
import org.example.model.game.Player;

import java.util.ArrayList;


public class ActionExecutionState extends State {

    private int offerIndex;
    private int remainingMoves;

    public ActionExecutionState(int offerIndex, int remainingMoves) {
        this.offerIndex = offerIndex;
        this.remainingMoves = remainingMoves;
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