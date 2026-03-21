package org.example.model.card.building.forEvent;

import org.example.model.card.Trigger;
import org.example.model.card.building.Building;
import org.example.model.game.Player;

import java.util.Optional;
import java.util.Set;

public abstract class DuringEvent extends Building {
    private final Set<Trigger> eventTriggers = Set.of(
            Trigger.SHAMANIC_RITUAL,
            Trigger.SUSTENANCE,
            Trigger.HUNT,
            Trigger.CAVE_PAINTINGS);

    public DuringEvent(int endGamePP, int cost, int era, Optional<Integer> numPlayers) {
        super(endGamePP, cost, era, numPlayers);
    }

    @Override
    public void activeEffect(Set<Player> players, Trigger t) {
        if (eventTriggers.contains(t)) {
            execute(players, t);
        }
    }

    public abstract void execute(Set<Player> players, Trigger t);
}

