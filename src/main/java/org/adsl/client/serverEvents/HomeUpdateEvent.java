package org.adsl.client.serverEvents;

import org.adsl.client.view.Screen;

import java.util.List;

public record HomeUpdateEvent(List<Integer> activeGames, String message) implements ServerEvent {

    public HomeUpdateEvent(List<Integer> activeGames) {
        this(activeGames, null);
    }

    @Override
    public <S extends Screen<S>> S accept(EventVisitor<S> visitor) {
        return visitor.visit(this);
    }
}
