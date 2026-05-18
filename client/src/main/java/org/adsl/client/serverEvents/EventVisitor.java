package org.adsl.client.serverEvents;

import org.adsl.client.view.Screen;
import org.adsl.client.view.tui.events.InputEventVisitor;
import org.adsl.shared.network.responses.TotemAvailableUpdate;

/**
 * Visitor interface for dispatching {@code ServerEvent} subtypes to the
 * correct view handler without instanceof checks. Implemented by both
 * {@code TUIScreen} and {@code GUIScreen} base classes.
 *
 * @param <S> the concrete screen type returned after handling the event
 */
public interface EventVisitor<S extends Screen<S>> {
    S visit(LoginNeededEvent event);
    S visit(HomeUpdateEvent event);
    S visit(LobbyUpdateEvent event);
    S visit(GameUpdateEvent event);
    S visit(EndGameEvent event);
    S visit(ErrorEvent event);
    S visit(DisconnectedEvent event);
    S visit(TotemAvailableEvent event);
    S visit(EventsTriggeredEvent event);
}
