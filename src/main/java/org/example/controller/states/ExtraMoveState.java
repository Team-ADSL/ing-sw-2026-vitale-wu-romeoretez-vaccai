package org.example.controller.states;

import org.example.shared.utils.Move;
import org.example.shared.enums.Row;
import org.example.shared.exceptions.InvalidMoveException;
import org.example.model.cards.Card;
import org.example.model.Game;
import org.example.model.Player;
import org.example.model.board.CardRow;
import org.example.model.board.OfferTrack;

import java.util.Set;

public class ExtraMoveState extends State {
    public ExtraMoveState(Game game) {
        super(game);
    }

    @Override
    public void checkMove(Set<Move> moves, Player p) throws InvalidMoveException {
        // ADD CONTROL CANBEPICKED FOR CARD TO PICK
        // Add controll on player turn
        OfferTrack offerTrack = getGame().getBoard().getOfferTrack();
        if(moves.size() != 1){
            throw new InvalidMoveException("Number of cards mismatch. Requires " + moves.size() + ", Allowed: " + 1);
        }
        Move move = moves.stream().findFirst().get();
        if(move.getRow() != Row.UPPER){
            throw new InvalidMoveException("Allowed only picking from TopRow");
        }
    }

    @Override
    public void execute(Set<Move> moves, Player p) {
        CardRow selectedRow = getGame().getBoard().getTopRow();
        assert moves.size() == 1;
        Move move = moves.stream().findFirst().get();
        Card selectedCard = selectedRow.pickCardAt(move.getRowIndex());
        selectedCard.insert(p.getCards());
    }

    @Override
    public State nextState() {
        return new EventsState(getGame());
    }
}
