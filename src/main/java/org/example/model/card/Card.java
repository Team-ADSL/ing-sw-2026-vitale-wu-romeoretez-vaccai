package org.example.model.card;

import java.util.Optional;

public abstract class Card {

    private final int era;
    private final Optional<Integer> numPlayers;

    public Card (int era, Optional<Integer> numPlayers) {

    this.era = era;
    this.numPlayers = numPlayers;

    }

}
