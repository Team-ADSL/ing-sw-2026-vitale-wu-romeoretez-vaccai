package org.adsl.server.model.cards.characters;

import org.adsl.server.model.cards.Card;
import org.adsl.shared.enums.CardType;

import java.util.Map;
import java.util.Set;

public class Shaman extends Character {

    private final int starNum;

    public Shaman(String id, int starNum, int era, Integer numPlayers) {
        super(id, era, numPlayers);
        this.starNum = starNum;
    }

    public int getStarNum() {
        return starNum;
    }

    @Override
    public void insert(Map<CardType, Set<Card>> cards) {
        cards.get(CardType.SHAMAN).add(this);
    }
}
