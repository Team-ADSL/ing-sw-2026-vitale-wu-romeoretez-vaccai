package org.adsl.shared.enums;

/**
 * Represents the current phase of a game round, stored in {@code Game} and
 * included in {@code GameDTO} so clients can render the appropriate UI state.
 * Used by {@code StateFactory} to reconstruct the correct controller state
 * after a server restart.
 */
public enum Phase {
    TOTEM_PICKING,
    TOTEM_PLACEMENT,
    ACTION_EXECUTION,
    EXTRA_MOVE,
    EVENTS_EXECUTION,
    END_ROUND,
    END_GAME
}
