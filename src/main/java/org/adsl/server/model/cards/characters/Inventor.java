package org.adsl.server.model.cards.characters;

import org.adsl.server.model.cards.Card;
import org.adsl.shared.enums.CardType;
import org.adsl.shared.enums.Icon;

import java.util.Map;
import java.util.Set;

public class Inventor extends Character {

    private final Icon icon;

    public Inventor (String id, Icon icon, int era, Integer numPlayers) {
        super(id, era, numPlayers);
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
