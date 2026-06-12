package org.adsl.server.model.cards.events;

import org.adsl.server.model.cards.Card;
import org.adsl.shared.enums.CardType;
import org.adsl.shared.enums.Trigger;
import org.adsl.server.model.cards.buildings.Building;
import org.adsl.server.model.Player;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Abstract base for event cards (Hunt, ShamanicRitual, Sustenance, CavePaintings).
 * <p>
 * Events cannot be drawn by players ({@link #canBeDrawn} always returns
 * {@code false}) and are instead resolved by {@code EventsState} when they are
 * visible on the lower (or upper, on round 10) card row.
 * </p>
 * <p>
 * Before applying the event's own effect, {@link #activateBuildings} is called
 * for each player so that any {@code DuringEvent} buildings can set their flags
 * in {@code BuildingBonus} before those flags are consumed.
 * </p>
 */
public abstract class Event extends Card {

    private final boolean isFinal;

    /**
     * Creates an event card.
     *
     * @param id         unique card identifier
     * @param isFinal    {@code true} if this is the era-3/round-10 copy of the
     *                   event (placed in the upper row instead of the lower one)
     * @param era        era this card belongs to
     * @param numPlayers number of players in the match, used to size player-dependent setups
     */
    public Event(String id, boolean isFinal, int era, Integer numPlayers) {
        super(id, era, numPlayers);
        this.isFinal = isFinal;
    }

    /**
     * Events are never part of a player's draw pool.
     *
     * @param p the player who would draw the card
     * @return always {@code false}
     */
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

    /**
     * Activates the given player's {@code Building} cards for the event trigger
     * {@code t}, allowing any matching {@code DuringEvent} building to set its
     * bonus flag in {@code BuildingBonus} before this event's own effect
     * consumes those flags.
     *
     * @param p the player whose buildings are activated
     * @param t the event-specific trigger (e.g. {@link Trigger#HUNT})
     */
    public void activateBuildings(Player p, Trigger t){
        p.getCards().get(CardType.BUILDINGS).stream()
                .map(c -> (Building)c)
                .forEach(b -> b.activeEffect(Collections.singleton(p), t));
    }

    /**
     * Event cards cost nothing to "play" since players never draw them.
     *
     * @return always {@code 0}
     */
    @Override
    public int getCost(){ return 0; }
}
