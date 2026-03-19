package org.example.model.game;

import java.util.Optional;

public class OrderCell {
    private Optional<Player> player;
    private int bonus;
    private boolean isMalus;

    public OrderCell(Optional<Player> player, int bonus, boolean isMalus) {
        this.player = player;
        this.bonus = bonus;
        this.isMalus = isMalus;
    }

    public Optional<Player> getPlayer() {
        return player;
    }
}
