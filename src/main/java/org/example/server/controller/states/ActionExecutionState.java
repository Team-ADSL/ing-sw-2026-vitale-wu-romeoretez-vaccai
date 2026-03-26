package org.example.server.controller.states;

import org.example.shared.utils.Move;
import org.example.shared.enums.Row;
import org.example.shared.exceptions.InvalidMoveException;
import org.example.server.model.cards.Card;
import org.example.shared.enums.CardType;
import org.example.shared.enums.Trigger;
import org.example.server.model.Game;
import org.example.server.model.Player;
import org.example.server.model.board.CardRow;
import org.example.server.model.board.OfferTile;
import org.example.server.model.board.OfferTrack;
import org.example.server.model.board.OrderCell;

import java.util.Map;
import java.util.Optional;
import java.util.Set;


public class ActionExecutionState extends State {

    public ActionExecutionState(Game game) {
        super(game);
    }

    @Override
    public void checkMove(Set<Move> moves, Player p) throws InvalidMoveException {
        // ADD CONTROL CANBEPICKED FOR EVERY CARD TO PICK
        OfferTrack offerTrack = getGame().getBoard().getOfferTrack();
        int offerIndex = 0;
        while(offerTrack.getTileAt(offerIndex).getPlayer().isEmpty() && offerIndex < offerTrack.size()){
            offerIndex++;
        }
        int remainingMoves = offerTrack.getTileAt(offerIndex).getNumMoves();
        if(moves.size() != remainingMoves){
            throw new InvalidMoveException(
                    "Number of cards mismatch. Requires " + moves.size()
                            + ", Allowed: " + remainingMoves);
        }
        OfferTile offerTile = getGame().getBoard().getOfferTrack().getTileAt(offerIndex);
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

    @Override
    public void execute(Set<Move> moves, Player p) {
        for(Move move : moves){
            CardRow selectedRow = null;
            if(move.getRow() == Row.UPPER) {
                selectedRow = getGame().getBoard().getTopRow();
            } else {
                selectedRow = getGame().getBoard().getLowRow();
            }
            Card selectedCard = selectedRow.pickCardAt(move.getRowIndex());
            selectedCard.insert(p.getCards());
        }

        int i = getGame().getBoard().getOrderTile().placePlayerAtNext(p);

        // Eventual bonus for totem placement in order tile
        p.getCards().get(CardType.BUILDINGS).forEach(b -> b.activeEffect(Set.of(p),Trigger.END_TURN));

        // Set new point/food after player's move
        OrderCell orderCell = getGame().getBoard().getOrderTile().getCellAt(i);
        if(orderCell.getBonus() >= 0){
            p.changeFood(orderCell.getBonus());
            if(p.getBuildingBonus().isBonusFoodTile()){
                p.changeFood(1);
            }
        } else if (orderCell.isMalus()){
            if(p.getFood() != 0){
                p.changeFood(-1);
            } else{
                p.changePP(-2);
            }
        }
    }

    @Override
    public State nextState() {
        OfferTrack offerTrack = getGame().getBoard().getOfferTrack();
        int offerIndex = 0;
        while(offerTrack.getTileAt(offerIndex).getPlayer().isEmpty() && offerIndex < offerTrack.size()){
            offerIndex++;
        }
        if(offerTrack.getTileAt(offerIndex).getPlayer().isPresent()){
            return this;
        }

        // Control eventual extra move
        getGame().getPlayers().forEach(
                p -> p.getCards().get(CardType.BUILDINGS).forEach(
                        b -> b.activeEffect(Set.of(p), Trigger.END_ROUND)));

        Optional<Player> playerExtraMove = getGame().getPlayers().stream()
                .filter(p -> p.getBuildingBonus().isExtraMove())
                .findFirst();

        if(playerExtraMove.isEmpty()){
            return new EventsState(getGame());
        } else {
            return new ExtraMoveState(getGame());
        }
    }
}