package org.example.server.model.cards.events;

import org.example.server.model.cards.Card;
import org.example.shared.enums.CardType;
import org.example.shared.enums.Trigger;
import org.example.server.model.cards.buildings.Building;
import org.example.server.model.Player;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public abstract class Event extends Card {

    private final boolean isFinal;

    public Event(boolean isFinal, int era, Optional<Integer> numPlayers) {
        super(era, numPlayers);
        this.isFinal = isFinal;
    }

    @Override
    public boolean canBeDrawn(Player p) {
        return false;
    }

    @Override
    public void insert(Map<CardType, Set<Card>> cards) {

    }

    public void activateBuildings(Player p, Trigger t){
        p.getCards().get(CardType.BUILDINGS).stream()
                .map(c -> (Building)c)
                .forEach(b -> b.activeEffect(Collections.singleton(p), t));
    }
}
