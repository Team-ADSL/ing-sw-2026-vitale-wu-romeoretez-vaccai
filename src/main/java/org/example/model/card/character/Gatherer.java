package org.example.model.card.character;

import org.example.model.card.Card;
import org.example.model.card.CardType;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class Gatherer extends Character {

    private final int discount;

    public Gatherer (int discount, int era, Optional<Integer> numPlayers) {
        super(era, numPlayers);
        this.discount = discount;
    }

    @Override
    public void insert(Map<CardType, Set<Card>> cards) {
        cards.get(CardType.GATHERER).add(this);
    }

    public int getDiscount() {
        return discount;
    }
}
