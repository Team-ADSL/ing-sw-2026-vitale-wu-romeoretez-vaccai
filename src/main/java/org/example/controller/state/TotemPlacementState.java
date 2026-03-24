package org.example.controller.state;

import org.example.controller.utils.Move;
import org.example.exception.InvalidMoveException;
import org.example.model.game.*;
import org.example.model.game.boardComponent.OfferTrack;
import org.example.model.game.boardComponent.OrderTile;

import java.util.Optional;
import java.util.Set;

public class TotemPlacementState extends State {

    private int orderIndex;

    public TotemPlacementState(Game game, int orderIndex) {
        super(game);
        this.orderIndex = orderIndex;
    }


    @Override
    public void checkMove(Set<Move> moves, Player p) throws  InvalidMoveException{
        if(moves.size() != 1){
            throw new InvalidMoveException("Invalid input, only 1 move allowed");
        }

        OrderTile orderTile = getGame().getBoard().getOrderTile();
        Optional<Player> playerContainer = orderTile.getPlayerAt(orderIndex);
        assert playerContainer.isPresent(); // NOTE: the current state always point to the next player
                                            // that needs to play.
        Player currentPlayer = playerContainer.get();
        if(!currentPlayer.equals(p) ) {
            throw new InvalidMoveException("The current player is " + currentPlayer.getName());
        }

        Move move = moves.stream().findFirst().orElse(null);
        if(move == null) {
            throw new InvalidMoveException("Move is null");
        }
    }

    @Override
    public void execute(Set<Move> moves, Player p) {
        Move move = moves.stream().findFirst().orElse(null);
        OfferTrack offerTrack = getGame().getBoard().getOfferTrack();
        assert move != null; // Already checked in checkMove()
        offerTrack.placeInOfferTile(p, move.getRowIndex());
    }

    @Override
    public State nextState() {
        OfferTrack offerTrack = getGame().getBoard().getOfferTrack();
        if(getOrderIndex() != getGame().getPlayers().size() - 1){
            setOrderIndex(getOrderIndex() + 1);
            return this;
        } else {
            // Create new state with first player
            Optional<Player> firstPlayer = Optional.empty();
            int offerIndex = 0;
            int queueSize = offerTrack.size();
            while (firstPlayer.isEmpty() && offerIndex < queueSize) {
                firstPlayer = offerTrack.getTileAt(offerIndex).getPlayer();
                if(firstPlayer.isEmpty()){
                    offerIndex++;
                }
            }
            int remainingMoves = offerTrack.getTileAt(offerIndex).getNumMoves();
            return new ActionExecutionState(getGame(), offerIndex, remainingMoves);
        }
    }

    public int getOrderIndex() {
        return orderIndex;
    }
    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }
}
