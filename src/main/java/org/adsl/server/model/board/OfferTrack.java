package org.adsl.server.model.board;

import org.adsl.server.model.Player;
import org.adsl.shared.enums.Totem;
import org.adsl.shared.model.OfferTileDTO;

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

    public ArrayList<OfferTileDTO> createDTO(){
        return (ArrayList<OfferTileDTO>) offerQueue.stream()
                .map(OfferTile::createDTO)
                .collect(Collectors.toList());
    }
}
