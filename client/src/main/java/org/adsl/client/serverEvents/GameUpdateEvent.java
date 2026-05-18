package org.adsl.client.serverEvents;

import org.adsl.client.view.Screen;

import org.adsl.shared.model.GameDTO;

/**
 * Server event carrying a full {@code GameDTO} snapshot after every game-state
 * change. The view replaces its entire model from this object.
 *
 * @param game    the updated game snapshot
 * @param message optional log message to append to the game log, or {@code null}
 */
public record GameUpdateEvent(GameDTO game, String message) implements ServerEvent {

    public GameUpdateEvent(GameDTO game) {
        this(game, null);
    }

    @Override
    public <S extends Screen<S>> S accept(EventVisitor<S> visitor) {
        return visitor.visit(this);
    }
}
