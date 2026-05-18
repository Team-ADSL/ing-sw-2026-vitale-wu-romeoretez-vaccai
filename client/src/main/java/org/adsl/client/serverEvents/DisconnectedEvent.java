package org.adsl.client.serverEvents;

import org.adsl.client.view.Screen;

/**
 * Server event fired when the connection to the server is lost, either due to
 * a timeout, an explicit server shutdown, or a network error. Routes to the
 * disconnected screen in both TUI and GUI.
 *
 * @param message human-readable description of the disconnection cause
 */
public record DisconnectedEvent(String message) implements ServerEvent {

    @Override
    public <S extends Screen<S>> S accept(EventVisitor<S> visitor) {
        return visitor.visit(this);
    }
}
