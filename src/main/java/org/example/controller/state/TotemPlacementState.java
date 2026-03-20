package org.example.controller.state;

import org.example.controller.state.utils.Move;
import org.example.exception.InvalidMoveException;
import org.example.model.game.*;

import java.util.Optional;
import java.util.Set;

public class TotemPlacementState extends State {

    private int orderIndex;

    public TotemPlacementState(Game game, int orderIndex) {
        super(game);
        this.orderIndex = orderIndex;
    }


    public boolean checkInput(Move move, Game game) {
        int i = move.getRowIndex();
        Optional<Player> playerContainer = game.getBoard().getOfferTrack().getPlayerAt(i);
        return playerContainer.isEmpty();
    }

    @Override
    public State transition(Set<Move> moves) throws InvalidMoveException {
        if(moves.size() != 1){
            throw new InvalidMoveException("Invalid input, only 1 move allowed");
        }

        OrderTile orderTile = getGame().getBoard().getOrderTile();
        Optional<Player> playerContainer = getGame().getBoard().getOrderTile().getPlayerAt(orderIndex);
        if(playerContainer.isEmpty()) {
            throw new InvalidMoveException("Player not present in the order queue");
        }
        Player player = playerContainer.get();

        Move move = moves.stream().findFirst().orElse(null);
        if(move == null){
            throw new InvalidMoveException("Move is null");
        }

        OfferTrack offerTrack = getGame().getBoard().getOfferTrack();
        playerContainer = offerTrack.getTileAt(move.getRowIndex()).getPlayer();
        if(playerContainer.isPresent()){
            throw new InvalidMoveException("Tiles already occupied in the offer track");
        }

        offerTrack.placeInOfferTile(player, move.getRowIndex());

        // Control end phase
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
