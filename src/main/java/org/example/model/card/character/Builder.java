package org.example.model.card.character;

import org.example.model.card.Card;
import org.example.model.card.CardType;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class Builder extends Character {

    private final int discount;
    private final int pp;

    public Builder(int discount, int pp, int era, Optional<Integer> numPlayers) {
        super(era, numPlayers);
        this.discount = discount;
        this.pp = pp;
    }

    @Override
    public void insert(Map<CardType, Set<Card>> cards) {
        cards.get(CardType.BUILDER).add(this);
    }

    public int getDiscount() {
        return discount;
    }

    public int getPP() {
        return pp;
    }
}
