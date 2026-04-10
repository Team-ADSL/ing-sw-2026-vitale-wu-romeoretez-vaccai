package org.example.server.model.board;

import org.example.shared.enums.Row;
import org.example.server.model.Player;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;

public class OfferTile implements Serializable {
    private Player player;
    private final Map<Row,Integer> moves;
    private final boolean givesFood;

    public OfferTile(Player player, Map<Row,Integer> moves, boolean givesFood) {
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

    public void setPlayer(Player player) {
        this.player = player;
    }
}
