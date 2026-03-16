package org.example.model.card.character;

import java.util.Optional;

public class Hunter extends Character {

    private boolean extraFood;

    public Hunter (boolean extraFood, int era, Optional<Integer> numPlayers) {
        super(era, numPlayers);
        this.extraFood = extraFood;
    }
}
