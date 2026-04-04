package org.example.server.model.cards.characters;

import org.example.server.model.cards.Card;
import org.example.shared.enums.CardType;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class Artist extends Character {

    public Artist (String id, int era, Optional<Integer> numPlayers) {
        super(id, era, numPlayers);
    }

    @Override
    public void insert(Map<CardType, Set<Card>> cards) {
        cards.get(CardType.ARTIST).add(this);
    }
}
