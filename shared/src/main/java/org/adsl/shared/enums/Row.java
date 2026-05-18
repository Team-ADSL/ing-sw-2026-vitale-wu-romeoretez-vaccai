package org.adsl.shared.enums;

/**
 * Identifies which row of the board a {@code Move} targets.
 * {@code UPPER} and {@code LOWER} refer to the two tribe-card rows;
 * {@code OFFER} refers to a slot on the offer track (used during totem placement).
 */
public enum Row {
    UPPER,
    LOWER,
    OFFER
}
