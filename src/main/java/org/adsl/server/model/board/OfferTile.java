package org.adsl.server.model.board;

import org.adsl.shared.enums.Row;
import org.adsl.server.model.Player;
import org.adsl.shared.model.OfferTileDTO;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;

public class OfferTile implements Serializable {
    private final String id;
    private Player player;
    private final Map<Row,Integer> moves;
    private final boolean givesFood;

    public OfferTile(String id, Player player, Map<Row,Integer> moves, boolean givesFood) {
        this.id = id;
        this.player = player;
        this.moves = moves;
        this.givesFood = givesFood;
    }

    public Optional<Player> getPlayer() {
        return Optional.ofNullable(player);
    }
    public int getNumMoves(){
        return moves.values().stream().mapToInt(Integer::intValue).sum();
    }
    public Map<Row, Integer> getMoves() {
        return moves;
    }
    public boolean isGivesFood() {
        return givesFood;
    }

    public OfferTileDTO createDTO(){
        return new OfferTileDTO(id, player.getColor());
    }

    public void setPlayer(Player player) {
        this.player = player;
    }
}
