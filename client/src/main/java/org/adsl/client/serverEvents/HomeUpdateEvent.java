package org.adsl.client.serverEvents;

import org.adsl.client.view.Screen;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Server event carrying the updated list of open game IDs when the home screen
 * roster changes (game created or ended), along with per-game player lists and
 * capacities for rich UI rendering.
 *
 * @param activeGames  current list of open game IDs
 * @param gamePlayers  map of gameId → player names currently in that lobby
 * @param gameCapacity map of gameId → total player slots
 * @param message      optional log message, or {@code null}
 */
public record HomeUpdateEvent(
        List<Integer> activeGames,
        Map<Integer, List<String>> gamePlayers,
        Map<Integer, Integer> gameCapacity,
        String message) implements ServerEvent {

    public HomeUpdateEvent(List<Integer> activeGames) {
        this(activeGames, Collections.emptyMap(), Collections.emptyMap(), null);
    }

    @Override
    public <S extends Screen<S>> S accept(EventVisitor<S> visitor) {
        return visitor.visit(this);
    }
}
