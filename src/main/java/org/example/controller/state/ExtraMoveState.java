package org.example.controller.state;

import org.example.controller.utils.Move;
import org.example.controller.utils.Row;
import org.example.exception.InvalidMoveException;
import org.example.model.card.Card;
import org.example.model.game.Game;
import org.example.model.game.Player;
import org.example.model.game.boardComponent.CardRow;
import org.example.model.game.boardComponent.OfferTrack;

import java.util.Map;
import java.util.Set;

public class ExtraMoveState extends State{
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
