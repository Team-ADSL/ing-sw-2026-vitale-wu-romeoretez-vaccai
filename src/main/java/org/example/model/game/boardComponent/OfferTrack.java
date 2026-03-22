package org.example.model.game.boardComponent;

import org.example.model.game.Player;

import java.util.ArrayList;

public class OfferTrack {
    private final ArrayList<OfferTile> offerQueue;

    public OfferTrack(ArrayList<OfferTile> offerQueue) {
        this.offerQueue = offerQueue;
    }

    public void placeInOfferTile(Player p, int arrayIndex){
        //
    }
    public OfferTile getTileAt(int i){
        return offerQueue.get(i);
    }

    public int size(){
        return offerQueue.size();
    }
}
