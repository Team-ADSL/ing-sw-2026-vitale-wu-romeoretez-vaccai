package org.adsl.server.model.cards.buildings.utils;

/**
 * Discriminator for the specific game effect applied by a {@code Building}
 * subclass. Used by {@code SinceBuilt}, {@code DuringRitual}, and {@code EndGame}
 * to select behaviour at runtime without requiring additional subclasses.
 */
public enum BuildingEffect {
    /** {@code SinceBuilt}: grants 5 food when the owner completes one card of every character type. */
    FOOD_COMPLETE_SET,
    /** {@code SinceBuilt}: grants 3 food each time the owner draws a second {@code Inventor}. */
    COUPLE_INVENTOR,

    /** {@code DuringRitual}: owner ignores the PP penalty when losing the shamanic ritual. */
    RITUAL_IMMUNITY,
    /** {@code DuringRitual}: owner gains +3 virtual shaman stars during the ritual. */
    RITUAL_STARS_BONUS,
    /** {@code DuringRitual}: owner doubles PP gained when winning the ritual alone. */
    RITUAL_DOUBLE_PP,

    /** {@code EndGame}: all building end-game PP are multiplied by 2. */
    END_BUILDER_MULTIPLIER,
    /** {@code EndGame}: grants 6 PP per card in the owner's smallest card-type group. */
    PP_COMPLETE_SET,
    /** {@code EndGame}: grants 1 PP per card of a specific character type. */
    END_CHARACTER_MULTIPLIER,
    /** {@code EndGame}: grants a flat 25 PP. */
    END_PP_BONUS,
}
