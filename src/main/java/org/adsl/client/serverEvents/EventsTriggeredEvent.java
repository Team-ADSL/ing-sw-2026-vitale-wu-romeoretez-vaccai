package org.adsl.client.serverEvents;

import org.adsl.client.view.Screen;

import java.util.List;

public record EventsTriggeredEvent(List<String> eventTitles, long durationMs) implements ServerEvent {

    @Override
    public <S extends Screen<S>> S accept(EventVisitor<S> visitor) {
        return visitor.visit(this);
    }
}
