package org.example.controller.state;

import org.example.model.game.Game;
import org.example.model.game.OfferTile;
import org.example.model.game.Player;

import java.util.Optional;

public class TotemPlacementState extends State {

    private int orderIndex;

    public TotemPlacementState(Player activePlayer, int orderIndex) {
        super(activePlayer);
        this.orderIndex = orderIndex;
    }

    @Override
    public void notifyClients(Game game) {
    }

    @Override
    public boolean checkInput(Move move, Game game) {
        int i = move.getRowIndex();
        OfferTile chosenTile = game.getBoard().getOfferQueue().get(i);
        return !chosenTile.getPlayer().isPresent();
    }

    @Override
    public State transition(Move move, Game game) {
        game.getBoard().placeInOfferTile(getPlayer(), move.getRowIndex());
        if(getOrderIndex() != game.getPlayers().size() - 1){
            setOrderIndex(getOrderIndex() + 1);
            return this;
        } else {
            Optional<Player> firstPlayer = Optional.empty();
            int offerIndex = 0;
            int queueSize = game.getBoard().getOfferQueue().size();
            while (!firstPlayer.isPresent() && offerIndex < queueSize) {
                firstPlayer = game.getBoard().getOfferQueue().get(offerIndex).getPlayer();
                if(firstPlayer.isPresent()){
                    offerIndex++;
                }
            }
            return new ActionExecutionState(offerIndex);
        }
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }
}
