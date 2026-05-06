package org.adsl.server.model.cards;

import org.adsl.shared.enums.CardType;
import org.adsl.shared.enums.Trigger;
import org.adsl.server.model.Player;
import org.adsl.shared.model.CardDTO;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public abstract class Card implements Serializable {

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

    protected String eraToRoman(int era) {
        return switch (era) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            default -> String.valueOf(era);
        };
    }

    protected abstract String getTypeLabel();
    protected abstract String getEffectsLabel();

    public CardDTO createDTO(){
        return new CardDTO(id, getTypeLabel(), getEffectsLabel());
    }

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
