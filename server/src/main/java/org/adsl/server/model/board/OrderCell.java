package org.adsl.server.model.board;

import org.adsl.server.model.Player;

import java.io.Serializable;
import java.util.Optional;

/**
 * A single slot on the {@link OrderTile}. Tracks which player occupies it and
 * the food/PP reward (or penalty) granted when a player's totem is placed here
 * at the end of their action.
 * <p>
 * A positive {@code bonus} grants that many food tokens. A negative {@code bonus}
 * with {@code isMalus = true} triggers the malus rule: the player loses 1 food
 * or, if out of food, 2 PP.
 * </p>
 */
public class OrderCell implements Serializable {
    private Player player;
    private final int bonus;
    private final boolean isMalus;

    public OrderCell(Player player, int bonus, boolean isMalus) {
        this.player = player;
        this.bonus = bonus;
        this.isMalus = isMalus;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public Optional<Player> getPlayer() {
        return Optional.ofNullable(player);
    }
    public int getBonus() {
        return bonus;
    }

    public boolean isMalus() {
        return isMalus;
    }
}
