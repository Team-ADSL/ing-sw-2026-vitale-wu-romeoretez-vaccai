package org.example.model.game.boardComponent;

import org.example.model.game.Player;

import java.util.Optional;

public class OrderCell {
    private Optional<Player> player;
    private final int bonus;
    private final boolean isMalus;

    public OrderCell(Optional<Player> player, int bonus, boolean isMalus) {
        this.player = player;
        this.bonus = bonus;
        this.isMalus = isMalus;
    }

    public Optional<Player> getPlayer() {
        return player;
    }
}
