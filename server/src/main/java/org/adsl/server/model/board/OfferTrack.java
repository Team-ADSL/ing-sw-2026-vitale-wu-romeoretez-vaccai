package org.adsl.server.model.board;

import org.adsl.server.model.Player;
import org.adsl.shared.enums.Totem;
import org.adsl.shared.model.OfferTileDTO;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The offer track: an ordered list of {@link OfferTile}s that defines both
 * the available actions and the order in which players execute them.
 * Players place their totem on a tile during {@code TotemPlacementState};
 * {@code ActionExecutionState} reads the track left-to-right to determine
 * whose turn it is.
 */
public class OfferTrack implements Serializable {
    private final ArrayList<OfferTile> offerQueue;

    /**
     * Creates an offer track backed by the given ordered list of tiles.
     *
     * @param offerQueue the offer tiles, in track order
     */
    public OfferTrack(ArrayList<OfferTile> offerQueue) {
        this.offerQueue = offerQueue;
    }

    /**
     * Places {@code p}'s totem on the offer tile at {@code arrayIndex}, declaring
     * the action they intend to perform this turn.
     *
     * @param p          the player placing their totem
     * @param arrayIndex index of the tile in the track
     */
    public void placeInOfferTile(Player p, int arrayIndex){
        offerQueue.get(arrayIndex).setPlayer(p);
    }

    /**
     * Removes {@code player}'s totem from whichever tile they currently occupy,
     * if any. Used when a player leaves the game or before re-placing their totem.
     *
     * @param player the player whose totem should be removed
     */
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

    /**
     * Converts this track to its DTO representation for sending to clients.
     *
     * @return the offer tiles' DTOs, in track order
     */
    public ArrayList<OfferTileDTO> createDTO(){
        return (ArrayList<OfferTileDTO>) offerQueue.stream()
                .map(OfferTile::createDTO)
                .collect(Collectors.toList());
    }
}
