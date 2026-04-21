package org.adsl.client.view.tui.events;

import org.adsl.client.view.tui.screens.Screen;

public interface EventVisitor {
    // Server events
    Screen visit(LoginNeededEvent event);
    Screen visit(HomeUpdateEvent event);
    Screen visit(LobbyUpdateEvent event);
    Screen visit(GameUpdateEvent event);
    Screen visit(EndGameEvent event);
    Screen visit(ErrorEvent event);

    // Input events (key presses translated to semantic events by TUI)
    Screen visit(ConfirmEvent event);
    Screen visit(SelectEvent event);
    Screen visit(NavigateLeftEvent event);
    Screen visit(NavigateRightEvent event);
    Screen visit(NavigateUpEvent event);
    Screen visit(NavigateDownEvent event);
    Screen visit(CharInputEvent event);
}
