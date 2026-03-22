package org.example.model.card.character;

import org.example.model.card.Card;
import org.example.model.card.CardType;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class Inventor extends Character {

    private final Icon icon;

    public Inventor (Icon icon, int era, Optional<Integer> numPlayers) {
        super(era, numPlayers);
        this.icon = icon;
    }

    @Override
    public void insert(Map<CardType, Set<Card>> cards) {
        cards.get(CardType.INVENTOR).add(this);
    }

    public Icon getIcon() {
        return icon;
    }
}
