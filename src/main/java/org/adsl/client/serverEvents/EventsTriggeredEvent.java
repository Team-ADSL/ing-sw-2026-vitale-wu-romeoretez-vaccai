package org.adsl.client.serverEvents;

import org.adsl.client.view.Screen;

public record EventsTriggeredEvent(String eventTitle) implements ServerEvent {

    @Override
    public <S extends Screen<S>> S accept(EventVisitor<S> visitor) {
        return visitor.visit(this);
    }
}
