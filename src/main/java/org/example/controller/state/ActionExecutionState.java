package org.example.controller.state;

import org.example.controller.state.utils.Move;
import org.example.controller.state.utils.Row;
import org.example.exception.InvalidMoveException;
import org.example.model.card.Card;
import org.example.model.game.Game;
import org.example.model.game.boardComponent.OfferTile;

import java.util.Map;
import java.util.Set;


public class ActionExecutionState extends State {

    private int offerIndex;
    private int remainingMoves;
    private boolean isExtraMove;

    public ActionExecutionState(Game game, int offerIndex, int remainingMoves) {
        super(game);
        this.offerIndex = offerIndex;
        this.remainingMoves = remainingMoves;
        this.isExtraMove = false;
    }

    @Override
    public State transition(Set<Move> moves) throws InvalidMoveException {
        if(moves.size() != remainingMoves){
            throw new InvalidMoveException(
                    "Number of cards mismatch. Requires " + moves.size()
                    + ", Allowed: " + remainingMoves);
        }

        OfferTile offerTile = getGame().getBoard().getOfferTrack().getTileAt(offerIndex);
        if(isExtraMove){
            throw new InvalidMoveException(
                    "Number of cards mismatch. Requires " + moves.size() + ", Allowed: " + 1);
        } else if(moves.size() != 1){
            Map<Row, Integer> allowedMoves = offerTile.getMoves();
            int numLowDraw = (int) moves.stream().filter(m -> m.getRow() == Row.LOWER).count();
            int numUpDraw = (int) moves.stream().filter(m -> m.getRow() == Row.UPPER).count();
            if(numUpDraw != allowedMoves.get(Row.UPPER) || numLowDraw != allowedMoves.get(Row.LOWER)){
                throw new InvalidMoveException(
                        "Wrong moves: you can do "
                                + allowedMoves.get(Row.UPPER) + " draws from the up row and "
                                + allowedMoves.get(Row.LOWER) + " draws from the low row");
            }
        }



        // Cicla sulle azioni richieste ed eseguile. Ricorda di controllare se puo essere pescata
        OfferTile.CardRow selectedRow = null;
        if(move.getRow() == Row.UPPER) {
            selectedRow = getGame().getBoard().getTopRow();
        } else {
            selectedRow = getGame().getBoard().getLowRow();
        }

        for(Move m : moves){

        }

        Card selectedCard = selectedRow.get(move.getRowIndex());
        selectedCard.insert(getActivePlayer().getCards());

        // Controllo fine fase: prima check di eventuali mosse bonus
        return null;
    }
}