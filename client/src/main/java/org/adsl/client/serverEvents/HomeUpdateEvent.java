package org.adsl.client.serverEvents;

import org.adsl.client.view.Screen;

import java.util.List;

/**
 * Server event carrying the updated list of open game IDs when the home screen
 * roster changes (game created or ended).
 *
 * @param activeGames current list of open game IDs
 * @param message     optional log message, or {@code null}
 */
public record HomeUpdateEvent(List<Integer> activeGames, String message) implements ServerEvent {

    public HomeUpdateEvent(List<Integer> activeGames) {
        this(activeGames, null);
    }

    @Override
    public <S extends Screen<S>> S accept(EventVisitor<S> visitor) {
        return visitor.visit(this);
    }
}
