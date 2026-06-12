package org.adsl.server.network;

import java.util.List;
import java.util.Map;

/**
 * Observer notified when the list of open games in the server lobby changes.
 * Implemented by {@link VirtualClient} to push {@code HomeUpdate} responses to
 * clients that are on the home screen.
 */
public interface HomeObserver {

    /**
     * Called when the list of open game IDs changes.
     *
     * @param activeGames current list of open game IDs
     */
    void updateHome(List<Integer> activeGames);

    /**
     * Called when the list of open game IDs changes with an optional log message.
     * Default implementation delegates to {@link #updateHome(List)}.
     *
     * @param activeGames current list of open game IDs
     * @param message     optional message to display in the client log
     */
    default void updateHome(List<Integer> activeGames, String message) {
        updateHome(activeGames);
    }

    /**
     * Called when the list of open games changes, including per-game player
     * lists and capacities. Default implementation delegates to
     * {@link #updateHome(List)}, ignoring the extra details.
     *
     * @param activeGames  current list of open game IDs
     * @param gamePlayers  map from game ID to the usernames of joined players
     * @param gameCapacity map from game ID to the maximum number of players allowed
     */
    default void updateHome(List<Integer> activeGames, Map<Integer, List<String>> gamePlayers, Map<Integer, Integer> gameCapacity) {
        updateHome(activeGames);
    }

    /**
     * Called when the list of open games changes, including per-game player
     * lists, capacities, and an optional log message. Default implementation
     * delegates to {@link #updateHome(List, String)}, ignoring the extra details.
     *
     * @param activeGames  current list of open game IDs
     * @param gamePlayers  map from game ID to the usernames of joined players
     * @param gameCapacity map from game ID to the maximum number of players allowed
     * @param message      optional message to display in the client log
     */
    default void updateHome(List<Integer> activeGames, Map<Integer, List<String>> gamePlayers, Map<Integer, Integer> gameCapacity, String message) {
        updateHome(activeGames, message);
    }
}
