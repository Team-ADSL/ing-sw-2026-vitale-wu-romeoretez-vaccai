package org.adsl.server.model.cards.buildings.forEvent;

import org.adsl.shared.enums.Trigger;
import org.adsl.server.model.cards.buildings.Building;
import org.adsl.server.model.Player;

import java.util.Set;

public abstract class DuringEvent extends Building {
    private final Set<Trigger> eventTriggers = Set.of(
            Trigger.SHAMANIC_RITUAL,
            Trigger.SUSTENANCE,
            Trigger.HUNT,
            Trigger.CAVE_PAINTINGS);

    public DuringEvent(String id, int endGamePP, int cost, int era, Integer numPlayers) {
        super(id, endGamePP, cost, era, numPlayers);
    }

    @Override
    public void activeEffect(Set<Player> players, Trigger t) {
        if (eventTriggers.contains(t)) {
            execute(players, t);
        }
    }

    public abstract void execute(Set<Player> players, Trigger t);
}

