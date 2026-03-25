package org.example.model.cards.characters;

import org.example.model.cards.Card;
import org.example.shared.enums.CardType;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class Shaman extends Character {

    private final int starNum;

    public Shaman(int starNum, int era, Optional<Integer> numPlayers) {
        super(era, numPlayers);
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
