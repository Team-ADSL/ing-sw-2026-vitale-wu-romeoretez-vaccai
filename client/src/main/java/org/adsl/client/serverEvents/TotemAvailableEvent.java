package org.adsl.client.serverEvents;

import org.adsl.client.view.Screen;
import org.adsl.shared.enums.Totem;

import java.util.List;

public record TotemAvailableEvent(List<Totem> totemList, String message) implements ServerEvent {
    @Override
    public <S extends Screen<S>> S accept(EventVisitor<S> visitor) {
        return visitor.visit(this);
    }
}
