package org.example.model.game;
import org.example.server.model.Player;
import org.example.server.model.cards.Card;
import org.example.shared.enums.CardType;
import org.example.shared.enums.Trigger;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

// Fake card only for testing purposes
public class FakeCard extends Card {
    public FakeCard() {
        super(1, Optional.empty());
    }
    @Override
    public boolean canBeDrawn(Player p) { return true; }
    @Override
    public void insert(Map<CardType, Set<Card>> cards) {}
    @Override
    public void activeEffect(Set<Player> players, Trigger t) {}
}