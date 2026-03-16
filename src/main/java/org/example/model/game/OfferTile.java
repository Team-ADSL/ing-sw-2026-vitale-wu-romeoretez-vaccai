package org.example.model.game;

import java.util.Optional;

public class OfferTile {
    private Optional<Player> player;
    private Action action;

    public OfferTile(Optional<Player> player, Action action) {
        this.player = player;
        this.action = action;
    }

}
