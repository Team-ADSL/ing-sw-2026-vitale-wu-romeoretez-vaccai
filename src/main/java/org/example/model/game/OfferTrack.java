package org.example.model.game;

import java.util.ArrayList;
import java.util.Optional;

public class OfferTrack {
    private ArrayList<OfferTile> offerQueue;

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
