package org.adsl.client.view.tui.events;

import org.adsl.client.view.tui.screens.DisconnectedScreen;
import org.adsl.client.view.tui.screens.Screen;

public interface EventVisitor {
    // Server events
    Screen visit(LoginNeededEvent event);
    Screen visit(HomeUpdateEvent event);
    Screen visit(LobbyUpdateEvent event);
    Screen visit(GameUpdateEvent event);
    Screen visit(EndGameEvent event);
    Screen visit(ErrorEvent event);

    /**
     * Default handler for server disconnection: always transitions to
     * {@link DisconnectedScreen}, regardless of the current screen.
     * Individual screens may override this if needed.
     */
    default Screen visit(DisconnectedEvent event) {
        return new DisconnectedScreen(
                event.getTerminal(),
                event.getGui(),
                event.getCoordinator(),
                event.getMessage());
    }

    // Input events (key presses translated to semantic events by TUI)
    Screen visit(ConfirmEvent event);
    Screen visit(SelectEvent event);
    Screen visit(NavigateLeftEvent event);
    Screen visit(NavigateRightEvent event);
    Screen visit(NavigateUpEvent event);
    Screen visit(NavigateDownEvent event);
    Screen visit(CharInputEvent event);
}
