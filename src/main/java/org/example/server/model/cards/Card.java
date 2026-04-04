package org.example.server.model.cards;

import org.example.shared.enums.CardType;
import org.example.shared.enums.Trigger;
import org.example.server.model.Player;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public abstract class Card {

    private final String id;
    private final int era;
    private final Integer numPlayers;

    public Card (String id, int era, Integer numPlayers) {
        this.id = id;
        this.era = era;
        this.numPlayers = numPlayers;
    }

    public abstract boolean canBeDrawn(Player p);
    public abstract void insert(Map<CardType, Set<Card>> cards);
    public abstract void activeEffect(Set<Player> players, Trigger t);

    public String getId() {
        return id;
    }

    public int getEra() {
        return era;
    }

    public Optional<Integer> getNumPlayers() {
        return Optional.ofNullable(numPlayers);
    }
}
