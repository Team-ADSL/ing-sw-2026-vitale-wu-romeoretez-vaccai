package org.adsl.utils.fakes;

import org.adsl.server.model.Player;
import org.adsl.server.model.cards.Card;
import org.adsl.shared.enums.CardType;
import org.adsl.shared.enums.Trigger;

import java.util.Map;
import java.util.Set;

public class FakeCard extends Card {
    public FakeCard() {
        super("id", 1, 5);
    }

    @Override
    public boolean canBeDrawn(Player p) {
        return true;
    }

    @Override
    public void insert(Map<CardType, Set<Card>> cards) {}

    @Override
    public void activeEffect(Set<Player> players, Trigger t) {}

    @Override
    public int getCost() {
        return 0;
    }

    @Override
    protected String getTypeLabel() { return "fake"; }
    @Override
    protected String getEffectsLabel() { return ""; }
}
