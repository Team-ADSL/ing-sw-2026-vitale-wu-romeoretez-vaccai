package org.adsl.server.network;

import java.util.List;

public interface HomeObserver {
    void updateHome(List<Integer> activeGames);

    default void updateHome(List<Integer> activeGames, String message) {
        updateHome(activeGames);
    }
}
