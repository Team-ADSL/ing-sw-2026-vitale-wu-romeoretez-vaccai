package org.adsl.server.model.board;

import org.adsl.shared.enums.Row;
import org.adsl.shared.enums.Totem;
import org.adsl.server.model.Player;
import org.adsl.shared.model.OfferTileDTO;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;

/**
 * A single slot on the offer track. Players place their totem here during
 * {@code TotemPlacementState} to declare which card rows they will draw from
 * and how many cards they may draw. A tile can grant food instead of cards
 * ({@code givesFood}), in which case the action is resolved automatically.
 */
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
        Totem totemColor = (player != null) ? player.getColor() : null;
        return new OfferTileDTO(id, totemColor, moves, givesFood);
    }

    public void setPlayer(Player player) {
        this.player = player;
    }
}
