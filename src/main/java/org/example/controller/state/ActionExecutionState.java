package org.example.controller.state;

import org.example.controller.state.utils.Move;
import org.example.controller.state.utils.Row;
import org.example.exception.InvalidMoveException;
import org.example.model.card.Card;
import org.example.model.card.CardType;
import org.example.model.card.Trigger;
import org.example.model.card.building.Building;
import org.example.model.card.character.Builder;
import org.example.model.game.Game;
import org.example.model.game.Player;
import org.example.model.game.boardComponent.CardRow;
import org.example.model.game.boardComponent.OfferTile;
import org.example.model.game.boardComponent.OfferTrack;
import org.example.model.game.boardComponent.OrderCell;

import java.util.Map;
import java.util.Optional;
import java.util.Set;


public class ActionExecutionState extends State {

    private int offerIndex;
    private int remainingMoves;
    private Optional<Player> playerExtraMove;

    public ActionExecutionState(Game game, int offerIndex, int remainingMoves) {
        super(game);
        this.offerIndex = offerIndex;
        this.remainingMoves = remainingMoves;
        this.playerExtraMove = Optional.empty();
    }

    @Override
    public void checkMove(Set<Move> moves, Player p) throws InvalidMoveException {
        // ADD CONTROL CANBEPICKED FOR EVERY CARD TO PICK
        if(moves.size() != remainingMoves){
            throw new InvalidMoveException(
                    "Number of cards mismatch. Requires " + moves.size()
                            + ", Allowed: " + remainingMoves);
        }

        OfferTile offerTile = getGame().getBoard().getOfferTrack().getTileAt(offerIndex);
        if(playerExtraMove.isEmpty()){
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
        } else {

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
        Optional<Player> nextPlayer = Optional.empty();
        int queueSize = offerTrack.size();
        while (nextPlayer.isEmpty() && offerIndex < queueSize) {
            nextPlayer = offerTrack.getTileAt(offerIndex).getPlayer();
            if(nextPlayer.isEmpty()){
                offerIndex++;
            }
        }

        if(nextPlayer.isPresent()){
            remainingMoves = offerTrack.getTileAt(offerIndex).getNumMoves();
            return this;
        }

        // Control eventual extra move
        getGame().getPlayers().forEach(
                p -> p.getCards().get(CardType.BUILDINGS).forEach(
                        b -> b.activeEffect(Set.of(p), Trigger.END_ROUND)));

        playerExtraMove = getGame().getPlayers().stream()
                .filter(p -> p.getBuildingBonus().isExtraMove())
                .findFirst();

        if(playerExtraMove.isEmpty()){
            return new EventsState(getGame());
        } else {
            return this;
        }
    }
}