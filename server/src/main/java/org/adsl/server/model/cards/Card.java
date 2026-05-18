package org.adsl.server.model.cards;

import org.adsl.shared.enums.CardType;
import org.adsl.shared.enums.Trigger;
import org.adsl.server.model.Player;
import org.adsl.shared.model.CardDTO;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Abstract base for all game cards (characters, events, buildings).
 * <p>
 * Subclasses must implement:
 * <ul>
 *   <li>{@link #canBeDrawn(Player)} – whether this card can be taken by the given player.</li>
 *   <li>{@link #insert(Map)} – adds itself to the correct bucket of a card-type map
 *       (used both for player hands and for event dispatch in {@code EventsState}).</li>
 *   <li>{@link #activeEffect(Set, Trigger)} – applies the card's game effect.</li>
 *   <li>{@link #getTypeLabel()} and {@link #getEffectsLabel()} – provide display
 *       strings for the {@code CardDTO} sent to clients.</li>
 * </ul>
 */
public abstract class Card implements Serializable {

    private final String id;
    private final int era;
    private final Integer numPlayers;

    public Card (String id, int era, Integer numPlayers) {
        this.id = id;
        this.era = era;
        this.numPlayers = numPlayers;
    }

    /**
     * Returns {@code true} if this card can be taken by {@code p}.
     * Pass {@code null} to test unconditional drawability (e.g. event cards
     * always return {@code false}; buildings check food against cost).
     *
     * @param p the player attempting to draw, or {@code null}
     * @return {@code true} if the card is available to that player
     */
    public abstract boolean canBeDrawn(Player p);
    /**
     * Adds this card to the appropriate bucket in {@code cards}. Used to
     * populate a player's hand and to sort visible board cards into typed
     * buckets for event dispatch.
     *
     * @param cards the card-type map to insert into
     */
    public abstract void insert(Map<CardType, Set<Card>> cards);
    /**
     * Applies this card's game effect to the given set of players when the
     * specified trigger fires.
     *
     * @param players the players affected (typically a singleton for character
     *                cards, the full player set for events)
     * @param t       the trigger that caused the effect to activate
     */
    public abstract void activeEffect(Set<Player> players, Trigger t);

    protected String eraToRoman(int era) {
        return switch (era) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            default -> String.valueOf(era);
        };
    }

    protected abstract String getTypeLabel();
    protected abstract String getEffectsLabel();
    protected String getCostLabel() { return null; }

    public CardDTO createDTO(){
        return new CardDTO(id, getTypeLabel(), getEffectsLabel(), getCostLabel());
    }

    public String getId() {
        return id;
    }
    public int getEra() {
        return era;
    }
    public Optional<Integer> getNumPlayers() {
        return Optional.ofNullable(numPlayers);
    }
}
