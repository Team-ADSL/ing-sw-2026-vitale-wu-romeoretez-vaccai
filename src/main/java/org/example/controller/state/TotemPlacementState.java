package org.example.controller.state;

import org.example.model.game.Game;
import org.example.model.game.OfferTile;
import org.example.model.game.Player;

import java.util.Optional;

public class TotemPlacementState extends State {

    private int orderIndex;

    public TotemPlacementState(Game game, int orderIndex) {
        super(game);
        this.orderIndex = orderIndex;
    }


    public boolean checkInput(Move move, Game game) {
        int i = move.getRowIndex();
        OfferTile chosenTile = game.getBoard().getOfferQueue().get(i);
        return !chosenTile.getPlayer().isPresent();
    }

    @Override
    public State transition(Move move) {
        getGame().getBoard().placeInOfferTile(...., move.getRowIndex());
        if(getOrderIndex() != getGame().getPlayers().size() - 1){
            setOrderIndex(getOrderIndex() + 1);
            return this;
        } else {
            Optional<Player> firstPlayer = Optional.empty();
            int offerIndex = 0;
            int queueSize = getGame().getBoard().getOfferQueue().size();
            while (!firstPlayer.isPresent() && offerIndex < queueSize) {
                firstPlayer = getGame().getBoard().getOfferQueue().get(offerIndex).getPlayer();
                if(firstPlayer.isPresent()){
                    offerIndex++;
                }
            }
            return new ActionExecutionState(getGame(), 0, offerIndex);
        }
    }

    public int getOrderIndex() {
        return orderIndex;
    }
    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }
}
