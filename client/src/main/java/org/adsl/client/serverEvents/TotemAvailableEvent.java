package org.adsl.client.serverEvents;

import org.adsl.client.view.Screen;
import org.adsl.shared.enums.Totem;

import java.util.List;

/**
 * Server event fired during the totem-picking phase, broadcasting the list
 * of totems still available for selection after each pick.
 *
 * @param totemList totems not yet chosen by any player
 * @param message   optional log message, or {@code null}
 */
public record TotemAvailableEvent(List<Totem> totemList, String message) implements ServerEvent {
    @Override
    public <S extends Screen<S>> S accept(EventVisitor<S> visitor) {
        return visitor.visit(this);
    }
}
