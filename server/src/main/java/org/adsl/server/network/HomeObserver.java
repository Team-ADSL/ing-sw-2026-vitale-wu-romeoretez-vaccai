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

    default void updateHome(List<Integer> activeGames, Map<Integer, List<String>> gamePlayers, Map<Integer, Integer> gameCapacity) {
        updateHome(activeGames);
    }

    default void updateHome(List<Integer> activeGames, Map<Integer, List<String>> gamePlayers, Map<Integer, Integer> gameCapacity, String message) {
        updateHome(activeGames, message);
    }
}
