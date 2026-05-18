package org.adsl.shared.enums;

/**
 * Timing events used to activate card effects. Each {@code Building} and some
 * {@code Character} subclasses react to a specific trigger value passed to
 * {@code Card.activeEffect()}.
 *
 * <ul>
 *   <li>{@code SUSTENANCE}, {@code HUNT}, {@code CAVE_PAINTINGS}, {@code SHAMANIC_RITUAL}
 *       – fired once per resolved event in {@code EventsState}.</li>
 *   <li>{@code EVENT_EXECUTION} – passed to events themselves when they resolve.</li>
 *   <li>{@code END_TURN} – fired after a player completes their action in
 *       {@code ActionExecutionState}.</li>
 *   <li>{@code END_ROUND} – fired at the start of {@code ExtraMoveState}.</li>
 *   <li>{@code END_GAME} – fired in {@code EndGameState}.</li>
 *   <li>{@code DRAWING} – fired immediately after a card is drawn by a player.</li>
 *   <li>{@code CHECK_BUILDINGS} – reserved for future building checks.</li>
 * </ul>
 */
public enum Trigger {
    SUSTENANCE,
    HUNT,
    CAVE_PAINTINGS,
    SHAMANIC_RITUAL,
    END_GAME,
    END_ROUND,
    END_TURN,
    DRAWING,
    CHECK_BUILDINGS,
    EVENT_EXECUTION
}
