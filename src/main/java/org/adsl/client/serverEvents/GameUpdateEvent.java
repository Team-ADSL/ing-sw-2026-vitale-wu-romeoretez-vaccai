package org.adsl.client.serverEvents;

import org.adsl.client.view.Screen;

import org.adsl.shared.model.GameDTO;

public record GameUpdateEvent(GameDTO game) implements ServerEvent {

    @Override
    public <S extends Screen<S>> S accept(EventVisitor<S> visitor) {
        return visitor.visit(this);
    }
}
