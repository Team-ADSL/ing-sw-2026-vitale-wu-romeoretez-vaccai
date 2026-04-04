package org.example.server.model.board;

import org.example.server.model.Player;

import java.util.ArrayList;

public class OfferTrack {
    private final ArrayList<OfferTile> offerQueue;

    public OfferTrack(ArrayList<OfferTile> offerQueue) {
        this.offerQueue = offerQueue;
    }

    public void placeInOfferTile(Player p, int arrayIndex){
        offerQueue.get(arrayIndex).setPlayer(p);
    }
    public OfferTile getTileAt(int i){
        return offerQueue.get(i);
    }

    public int size(){
        return offerQueue.size();
    }
}
