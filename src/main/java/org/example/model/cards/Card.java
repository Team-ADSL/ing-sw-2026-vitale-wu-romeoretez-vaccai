package org.example.model.cards;

import org.example.shared.enums.CardType;
import org.example.shared.enums.Trigger;
import org.example.model.Player;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public abstract class Card {

    private final int era;
    private final Optional<Integer> numPlayers;

    public Card (int era, Optional<Integer> numPlayers) {
        this.era = era;
        this.numPlayers = numPlayers;
    }

    public abstract boolean canBeDrawn(Player p);
    public abstract void insert(Map<CardType, Set<Card>> cards);
    public abstract void activeEffect(Set<Player> players, Trigger t);

    public int getEra() {
        return era;
    }

    public Optional<Integer> getNumPlayers() {
        return numPlayers;
    }
}
