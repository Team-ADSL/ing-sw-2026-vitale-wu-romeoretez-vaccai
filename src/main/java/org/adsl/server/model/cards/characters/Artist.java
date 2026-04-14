package org.adsl.server.model.cards.characters;

import org.adsl.server.model.cards.Card;
import org.adsl.shared.enums.CardType;

import java.util.Map;
import java.util.Set;

public class Artist extends Character {

    public Artist (String id, int era, Integer numPlayers) {
        super(id, era, numPlayers);
    }

    @Override
    public void insert(Map<CardType, Set<Card>> cards) {
        if(cards.containsKey(CardType.ARTIST)) cards.get(CardType.ARTIST).add(this);
    }
}
