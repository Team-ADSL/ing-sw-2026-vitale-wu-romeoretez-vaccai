package org.adsl.server.model.cards.characters;

import org.adsl.server.model.cards.Card;
import org.adsl.shared.enums.CardType;

import java.util.Map;
import java.util.Set;

public class Gatherer extends Character {

    private final int discount;

    public Gatherer (String id, int discount, int era, Integer numPlayers) {
        super(id, era, numPlayers);
        this.discount = discount;
    }

    @Override
    public void insert(Map<CardType, Set<Card>> cards) {
        if(cards.containsKey(CardType.GATHERER)) cards.get(CardType.GATHERER).add(this);
    }

    public int getDiscount() {
        return discount;
    }
}
