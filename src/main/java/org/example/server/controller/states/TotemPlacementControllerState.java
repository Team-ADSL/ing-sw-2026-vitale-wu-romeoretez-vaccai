package org.example.server.controller.states;

import org.example.shared.utils.Move;
import org.example.shared.exceptions.InvalidMoveException;
import org.example.server.model.Game;
import org.example.server.model.Player;
import org.example.server.model.board.OfferTrack;
import org.example.server.model.board.OrderTile;

import java.util.Optional;
import java.util.Set;

public class TotemPlacementControllerState extends ControllerState {

    public TotemPlacementControllerState(Game game) {
        super(game);
    }


    @Override
    public void checkMove(Set<Move> moves, Player p) throws  InvalidMoveException{
        if(moves.size() != 1){
            throw new InvalidMoveException("Invalid input, only 1 move allowed");
        }

        OrderTile orderTile = getGame().getBoard().getOrderTile();
        int orderIndex = 0;
        while(orderTile.getPlayerAt(orderIndex).isEmpty() && orderIndex < orderTile.size()){
            orderIndex++;
        }
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
    public ControllerState nextState() {
        OrderTile orderTile = getGame().getBoard().getOrderTile();
        int orderIndex = 0;
        while(orderTile.getPlayerAt(orderIndex).isEmpty() && orderIndex < orderTile.size()){
            orderIndex++;
        }
        if(orderIndex != getGame().getPlayers().size() - 1){
            return this;
        } else {
            // Create new state with first player
            OfferTrack offerTrack = getGame().getBoard().getOfferTrack();
            Optional<Player> firstPlayer = Optional.empty();
            int offerIndex = 0;
            int queueSize = offerTrack.size();
            while (firstPlayer.isEmpty() && offerIndex < queueSize) {
                firstPlayer = offerTrack.getTileAt(offerIndex).getPlayer();
                if(firstPlayer.isEmpty()){
                    offerIndex++;
                }
            }
            return new ActionExecutionControllerState(getGame());
        }
    }
}
