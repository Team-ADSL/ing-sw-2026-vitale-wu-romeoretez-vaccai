package org.adsl.server.model.cards.buildings.forEvent;

import org.adsl.shared.enums.Trigger;
import org.adsl.server.model.cards.buildings.Building;
import org.adsl.server.model.Player;

import java.util.Set;

/**
 * Abstract base for buildings whose effect activates during an event resolution
 * (i.e. when one of {@link Trigger#HUNT}, {@link Trigger#SHAMANIC_RITUAL},
 * {@link Trigger#SUSTENANCE}, or {@link Trigger#CAVE_PAINTINGS} fires).
 * <p>
 * Subclasses implement {@link #execute} to define the specific bonus; this class
 * filters the incoming trigger against the set of event triggers before delegating.
 * </p>
 */
public abstract class DuringEvent extends Building {
    private final Set<Trigger> eventTriggers = Set.of(
            Trigger.SHAMANIC_RITUAL,
            Trigger.SUSTENANCE,
            Trigger.HUNT,
            Trigger.CAVE_PAINTINGS);

    /**
     * Creates an event-related building.
     *
     * @param id         unique card identifier
     * @param endGamePP  end-game PP awarded by this building
     * @param cost       food cost to acquire this building
     * @param era        era this card belongs to
     * @param numPlayers number of players in the match
     */
    public DuringEvent(String id, int endGamePP, int cost, int era, Integer numPlayers) {
        super(id, endGamePP, cost, era, numPlayers);
    }

    /**
     * Forwards to {@link #execute} only if {@code t} is one of the four event
     * triggers; ignores all other triggers (e.g. building-placement triggers).
     *
     * @param players the players affected
     * @param t       the trigger that fired
     */
    @Override
    public void activeEffect(Set<Player> players, Trigger t) {
        if (eventTriggers.contains(t)) {
            execute(players, t);
        }
    }

    /**
     * Applies the building's event-specific bonus. Only called when {@code t}
     * is one of the event triggers accepted by this subclass.
     *
     * @param players the players affected by the event
     * @param t       the event trigger that fired
     */
    public abstract void execute(Set<Player> players, Trigger t);
}

