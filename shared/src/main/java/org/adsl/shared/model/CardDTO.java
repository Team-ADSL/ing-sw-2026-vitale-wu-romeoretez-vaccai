package org.adsl.shared.model;

import java.io.Serializable;

/**
 * Immutable display data for a single card, sent to clients as part of
 * {@code BoardDTO} or {@code PlayerDTO}. All fields are pre-rendered strings
 * produced by the server-side card model.
 *
 * @param id           unique card identifier (from JSON config)
 * @param typeLabel    short type string rendered in the card header (e.g. {@code "[HUNTER] I"})
 * @param effectsLabel description of the card's effect (e.g. {@code "[MEAT]"})
 * @param costLabel    food cost string for buildings, {@code null} for characters/events
 */
public record CardDTO(String id, String typeLabel, String effectsLabel, String costLabel) implements Serializable {}