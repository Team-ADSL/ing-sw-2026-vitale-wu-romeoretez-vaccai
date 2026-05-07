package org.adsl.client.view.events;

import org.adsl.client.view.gui.screens.GUIScreen;

/**
 * Visitor consumed by GUI screens. Mirrors the server-event subset of the
 * TUI {@link org.adsl.client.view.tui.events.EventVisitor}; input events
 * are not surfaced to the GUI because JavaFX handles input directly.
 */
public interface GUIEventVisitor {
    GUIScreen visit(LoginNeededEvent event);
    GUIScreen visit(HomeUpdateEvent event);
    GUIScreen visit(LobbyUpdateEvent event);
    GUIScreen visit(GameUpdateEvent event);
    GUIScreen visit(EndGameEvent event);
    GUIScreen visit(ErrorEvent event);
    GUIScreen visit(DisconnectedEvent event);
}
