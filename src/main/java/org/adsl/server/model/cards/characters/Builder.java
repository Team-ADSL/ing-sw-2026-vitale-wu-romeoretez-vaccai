package org.adsl.server.model.cards.characters;

import org.adsl.server.model.cards.Card;
import org.adsl.shared.enums.CardType;
import org.adsl.shared.model.CardToken;

import java.util.Map;
import java.util.Set;

public class Builder extends Character {

    private final int discount;
    private final int pp;

    public Builder(String id, int discount, int pp, int era, Integer numPlayers) {
        super(id, era, numPlayers);
        this.discount = discount;
        this.pp = pp;
    }

    @Override
    public void insert(Map<CardType, Set<Card>> cards) {
        if(cards.containsKey(CardType.BUILDER))cards.get(CardType.BUILDER).add(this);
    }

    public int getDiscount() {
        return discount;
    }

    public int getPP() {
        return pp;
    }

    @Override
    protected String getTypeLabel() {
        return CardToken.BUILDER + " " + eraToRoman(getEra());
    }

    @Override
    protected String getEffectsLabel() {
        return "-" + discount + CardToken.FOOD + " " + pp + CardToken.PP;
    }
}
