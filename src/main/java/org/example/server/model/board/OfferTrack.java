package org.example.server.model.board;

import org.example.server.model.Player;
import org.example.shared.enums.Totem;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;

public class OfferTrack implements Serializable {
    private final ArrayList<OfferTile> offerQueue;

    public OfferTrack(ArrayList<OfferTile> offerQueue) {
        this.offerQueue = offerQueue;
    }

    public void placeInOfferTile(Player p, int arrayIndex){
        offerQueue.get(arrayIndex).setPlayer(p);
    }

    public void removePlayer(Player player){
        for (OfferTile offerTile : offerQueue) {
            Optional<Player> cellPlayer = offerTile.getPlayer();
            if (cellPlayer.isPresent()) {
                if (cellPlayer.get().equals(player)) {
                    offerTile.setPlayer(null);
                }
            }
        }
    }

    public OfferTile getTileAt(int i){
        return offerQueue.get(i);
    }

    public int size(){
        return offerQueue.size();
    }

    public ArrayList<Totem> createDTO(){
        return (ArrayList<Totem>) offerQueue.stream()
                .map(tile -> tile.getPlayer().orElse(null))
                .map(p -> p != null ? p.getColor() : null)
                .collect(Collectors.toList());
    }
}
