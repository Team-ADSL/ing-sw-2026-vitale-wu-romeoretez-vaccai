package org.example.model.board;

import org.example.model.Player;

import java.util.ArrayList;
import java.util.Optional;

public class OfferTrack {
    private final ArrayList<OfferTile> offerQueue;

    public OfferTrack(ArrayList<OfferTile> offerQueue) {
        this.offerQueue = offerQueue;
    }

    public void placeInOfferTile(Player p, int arrayIndex){
        offerQueue.get(arrayIndex).setPlayer(Optional.of(p));
    }
    public OfferTile getTileAt(int i){
        return offerQueue.get(i);
    }

    public int size(){
        return offerQueue.size();
    }
}
