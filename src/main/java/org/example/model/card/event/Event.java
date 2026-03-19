package org.example.model.card.event;

import org.example.model.card.Card;
import org.example.model.game.Player;

import java.util.Optional;

public abstract class Event extends Card {

    private boolean isFinal;

    public Event(boolean isFinal, int era, Optional<Integer> numPlayers) {

        super(era, numPlayers);

        this.isFinal = isFinal;

    }

    @Override
    public boolean canBeDrawn(Player p) {
        return false;
    }
}
