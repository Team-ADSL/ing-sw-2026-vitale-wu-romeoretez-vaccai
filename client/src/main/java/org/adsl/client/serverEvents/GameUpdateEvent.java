package org.adsl.client.serverEvents;

import org.adsl.client.view.Screen;

import org.adsl.shared.model.GameDTO;

public record GameUpdateEvent(GameDTO game, String message) implements ServerEvent {

    public GameUpdateEvent(GameDTO game) {
        this(game, null);
    }

    @Override
    public <S extends Screen<S>> S accept(EventVisitor<S> visitor) {
        return visitor.visit(this);
    }
}
