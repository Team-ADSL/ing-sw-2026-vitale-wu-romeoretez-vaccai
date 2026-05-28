package org.adsl.server.model.game;
import org.adsl.server.model.Player;
import org.adsl.server.model.cards.Card;
import org.adsl.shared.enums.CardType;
import org.adsl.shared.enums.Trigger;
import java.util.Map;
import java.util.Set;

// Fake card only for testing purposes
public class FakeCard extends Card {
    public FakeCard() {
        super("fake_card", 1, null);
    }
    @Override
    public boolean canBeDrawn(Player p) { return true; }
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