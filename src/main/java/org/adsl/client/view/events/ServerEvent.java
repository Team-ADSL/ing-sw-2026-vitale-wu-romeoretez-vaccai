package org.adsl.client.view.events;

import org.adsl.client.view.gui.screens.GUIScreen;

/**
 * Server-derived events handled by both view layers. In addition to the
 * TUI accept inherited from {@link Event}, a server event accepts a
 * {@link GUIEventVisitor} returning a {@link GUIScreen}.
 */
public abstract class ServerEvent extends Event {
    public abstract GUIScreen accept(GUIEventVisitor visitor);
}
