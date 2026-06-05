package org.adsl.server.model.cards.characters;

import org.adsl.server.model.cards.Card;
import org.adsl.shared.enums.CardType;
import org.adsl.shared.model.CardToken;

import java.util.Map;
import java.util.Set;

/**
 * Character card. Artists score 10 PP for every pair at end-game — an odd
 * leftover artist scores nothing, so 3 artists still yield 10 PP (resolved in
 * {@code EndGameState}). They also interact with the {@code CavePaintings} event
 * and the {@code DuringPaintings} building.
 */
public class Artist extends Character {

    public Artist (String id, int era, Integer numPlayers) {
        super(id, era, numPlayers);
    }

    @Override
    public void insert(Map<CardType, Set<Card>> cards) {
        if(cards.containsKey(CardType.ARTIST)) cards.get(CardType.ARTIST).add(this);
    }

    @Override
    protected String getTypeLabel() {
        return CardToken.ARTIST + " " + eraToRoman(getEra());
    }

    @Override
    protected String getEffectsLabel() {
        return "";
    }
}
