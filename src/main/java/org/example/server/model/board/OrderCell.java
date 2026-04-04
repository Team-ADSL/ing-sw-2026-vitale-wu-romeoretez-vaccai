package org.example.server.model.board;

import org.example.server.model.Player;

import java.util.Optional;

public class OrderCell {
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
