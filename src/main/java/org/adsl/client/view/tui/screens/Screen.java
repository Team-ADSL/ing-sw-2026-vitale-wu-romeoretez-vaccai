package org.adsl.client.view.tui.screens;

import org.adsl.client.view.tui.events.*;

import java.io.IOException;

/**
 * State in the TUI state machine. Each concrete screen represents one phase
 * of interaction. Both server responses and key presses are represented as
 * {@link Event} objects and dispatched through this visitor interface.
 * A visit method returns {@code this} to stay on the current screen, or a new
 * {@link Screen} instance to transition.
 */
public interface Screen extends EventVisitor {

    // Default: unhandled server events keep the current screen
    default Screen visit(LoginNeededEvent e)   { return this; }
    default Screen visit(HomeUpdateEvent e)    { return this; }
    default Screen visit(LobbyUpdateEvent e)   { return this; }
    default Screen visit(GameUpdateEvent e)    { return this; }
    default Screen visit(EndGameEvent e)       { return this; }
    default Screen visit(ErrorEvent e)         { return this; }

    // Default: unhandled input events keep the current screen
    default Screen visit(ConfirmEvent e)       { return this; }
    default Screen visit(SelectEvent e)        { return this; }
    default Screen visit(NavigateLeftEvent e)  { return this; }
    default Screen visit(NavigateRightEvent e) { return this; }
    default Screen visit(NavigateUpEvent e)    { return this; }
    default Screen visit(NavigateDownEvent e)  { return this; }
    default Screen visit(CharInputEvent e)     { return this; }

    default Screen handleEvent(Event event) { return event.accept(this); }

    void render() throws IOException;

    default void setToRender(boolean value) {}

    /**
     * Called once when the TUI transitions to this screen.
     * Returns {@code null} to stay on this screen, or a new {@link Screen}
     * instance to redirect immediately (e.g. back navigation, exit).
     */
    default Screen onEnter() throws Exception { return null; }

    boolean isToRender();
}
