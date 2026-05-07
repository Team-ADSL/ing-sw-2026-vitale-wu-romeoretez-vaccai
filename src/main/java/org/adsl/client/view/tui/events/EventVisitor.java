package org.adsl.client.view.tui.events;

import org.adsl.client.view.events.DisconnectedEvent;
import org.adsl.client.view.events.EndGameEvent;
import org.adsl.client.view.events.ErrorEvent;
import org.adsl.client.view.events.GameUpdateEvent;
import org.adsl.client.view.events.HomeUpdateEvent;
import org.adsl.client.view.events.LobbyUpdateEvent;
import org.adsl.client.view.events.LoginNeededEvent;
import org.adsl.client.view.tui.screens.TUIScreen;

public interface EventVisitor {
    // Server events
    TUIScreen visit(LoginNeededEvent event);
    TUIScreen visit(HomeUpdateEvent event);
    TUIScreen visit(LobbyUpdateEvent event);
    TUIScreen visit(GameUpdateEvent event);
    TUIScreen visit(EndGameEvent event);
    TUIScreen visit(ErrorEvent event);
    TUIScreen visit(DisconnectedEvent event);

    // Input events (key presses translated to semantic events by TUI)
    TUIScreen visit(ConfirmEvent event);
    TUIScreen visit(SelectEvent event);
    TUIScreen visit(NavigateLeftEvent event);
    TUIScreen visit(NavigateRightEvent event);
    TUIScreen visit(NavigateUpEvent event);
    TUIScreen visit(NavigateDownEvent event);
    TUIScreen visit(CharInputEvent event);
    TUIScreen visit(BackspaceEvent event);
}
