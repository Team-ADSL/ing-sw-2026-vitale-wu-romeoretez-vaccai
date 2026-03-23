package org.example.model.card.event;

import org.example.model.card.Card;
import org.example.model.card.CardType;
import org.example.model.card.Trigger;
import org.example.model.card.building.Building;
import org.example.model.game.Player;

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
