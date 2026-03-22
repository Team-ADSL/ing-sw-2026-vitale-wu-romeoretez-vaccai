package org.example.model.card.character;

import org.example.model.card.Card;
import org.example.model.card.CardType;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class Artist extends Character {

    public Artist ( int era, Optional<Integer> numPlayers) {
        super(era, numPlayers);
    }

    @Override
    public void insert(Map<CardType, Set<Card>> cards) {
        cards.get(CardType.ARTIST).add(this);
    }
}
