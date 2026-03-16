package org.example.model.card.character;

import java.util.Optional;

public class Gatherer extends Character {

    private int discount;

    public Gatherer (int discount, int era, Optional<Integer> numPlayers) {
        super(era, numPlayers);
        this.discount = discount;
    }
}
