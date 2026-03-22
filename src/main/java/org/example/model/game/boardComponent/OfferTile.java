package org.example.model.game.boardComponent;

import org.example.controller.state.utils.Row;
import org.example.model.card.Card;
import org.example.model.game.Player;

import java.util.Map;
import java.util.Optional;

public class OfferTile {
    private Optional<Player> player;
    private final Map<Row,Integer> moves;
    private final boolean givesFood;

    public OfferTile(Optional<Player> player, Map<Row,Integer> moves, boolean givesFood) {
        this.player = player;
        this.moves = moves;
        this.givesFood = givesFood;
    }

    public Optional<Player> getPlayer() {
        return player;
    }

    public int getNumMoves(){
        return moves.values().stream().mapToInt(Integer::intValue).sum();
    }

    public Map<Row, Integer> getMoves() {
        return moves;
    }
}
