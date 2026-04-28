package org.adsl.client.view.tui.events;

import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import org.adsl.client.AppCoordinator;
import org.adsl.client.view.tui.screens.Screen;

/**
 * Enqueued by {@link org.adsl.client.view.GameUI#onServerDisconnected()} when the
 * server connection is lost. Carries all context needed to build the
 * {@link org.adsl.client.view.tui.screens.DisconnectedScreen} inside the default
 * visitor implementation, so that every {@link Screen} automatically transitions
 * to the disconnection screen without per-screen override.
 */
public class DisconnectedEvent extends Event {

    private final com.googlecode.lanterna.screen.Screen terminal;
    private final WindowBasedTextGUI gui;
    private final AppCoordinator coordinator;
    private final String message;

    public DisconnectedEvent(com.googlecode.lanterna.screen.Screen terminal,
                             WindowBasedTextGUI gui,
                             AppCoordinator coordinator,
                             String message) {
        this.terminal    = terminal;
        this.gui         = gui;
        this.coordinator = coordinator;
        this.message     = message;
    }

    public com.googlecode.lanterna.screen.Screen getTerminal()  { return terminal; }
    public WindowBasedTextGUI                    getGui()       { return gui; }
    public AppCoordinator                        getCoordinator(){ return coordinator; }
    public String                                getMessage()   { return message; }

    @Override
    public Screen accept(EventVisitor visitor) {
        return visitor.visit(this);
    }
}
