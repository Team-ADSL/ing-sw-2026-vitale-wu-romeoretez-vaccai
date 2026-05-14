package org.adsl.server.model.cards.events;

import org.adsl.server.model.cards.Card;
import org.adsl.shared.enums.CardType;
import org.adsl.shared.enums.Trigger;
import org.adsl.server.model.cards.buildings.Building;
import org.adsl.server.model.Player;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public abstract class Event extends Card {

    private final boolean isFinal;

    public Event(String id, boolean isFinal, int era, Integer numPlayers) {
        super(id, era, numPlayers);
        this.isFinal = isFinal;
    }

    @Override
    public boolean canBeDrawn(Player p) {
        return false;
    }

    /**
     * Human-readable title used by the events overlay / log (e.g. "HUNT",
     * "SHAMANIC RITUAL"). Defined here so callers dispatch via polymorphism
     * instead of branching on the runtime subtype.
     */
    public abstract String getEventTitle();

    public void activateBuildings(Player p, Trigger t){
        p.getCards().get(CardType.BUILDINGS).stream()
                .map(c -> (Building)c)
                .forEach(b -> b.activeEffect(Collections.singleton(p), t));
    }
}
