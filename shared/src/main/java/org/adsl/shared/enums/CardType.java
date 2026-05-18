package org.adsl.shared.enums;

/**
 * Classifies every card type used in the game. Also serves as the key for
 * per-player card maps and for event-dispatch buckets in {@code EventsState}.
 * Character types ({@code HUNTER}–{@code BUILDER}) map to character cards;
 * event types ({@code HUNT}, {@code SUSTENANCE}, {@code SHAMANIC_RITUAL},
 * {@code CAVE_PAINTINGS}) map to event cards; {@code BUILDINGS} maps to
 * building cards.
 */
public enum CardType {
    INVENTOR,
    ARTIST,
    HUNTER,
    SHAMAN,
    GATHERER,
    BUILDER,
    BUILDINGS,
    SHAMANIC_RITUAL,
    SUSTENANCE,
    CAVE_PAINTINGS,
    HUNT
}
