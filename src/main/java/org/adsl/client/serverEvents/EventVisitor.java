package org.adsl.client.serverEvents;

import org.adsl.client.view.Screen;
import org.adsl.client.view.tui.events.InputEventVisitor;
import org.adsl.shared.network.responses.TotemAvailableUpdate;

public interface EventVisitor<S extends Screen<S>> {
    S visit(LoginNeededEvent event);
    S visit(HomeUpdateEvent event);
    S visit(LobbyUpdateEvent event);
    S visit(GameUpdateEvent event);
    S visit(EndGameEvent event);
    S visit(ErrorEvent event);
    S visit(DisconnectedEvent event);
    S visit(TotemAvailableEvent event);
}
