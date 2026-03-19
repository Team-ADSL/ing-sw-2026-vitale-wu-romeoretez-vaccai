package org.example.model.game;

import org.example.controller.state.Row;

import java.util.Map;
import java.util.Optional;

public class OfferTile {
    private Optional<Player> player;
    private Map<Row,Integer> moves;
    private boolean givesFood;

    public OfferTile(Optional<Player> player, Map<Row,Integer> moves, boolean givesFood) {
        this.player = player;
        this.moves = moves;
        this.givesFood = givesFood;
    }

    public Optional<Player> getPlayer() {
        return player;
    }
}
