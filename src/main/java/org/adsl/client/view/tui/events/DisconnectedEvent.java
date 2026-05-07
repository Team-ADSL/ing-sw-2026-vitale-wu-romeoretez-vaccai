package org.adsl.client.view.tui.events;

import org.adsl.client.view.tui.screens.TUIScreen;

/**
 * Enqueued by {@link org.adsl.client.view.GameUI#onServerDisconnected()} when the
 * server connection is lost. The default visitor on {@link TUIScreen} builds the
 * {@link org.adsl.client.view.tui.screens.DisconnectedScreen} reusing the screen's
 * own terminal and coordinator references, so the event itself only needs to
 * carry the human-readable diagnostic message.
 */
public class DisconnectedEvent extends Event {

    private final String message;

    public DisconnectedEvent(String message) {
        this.message = message;
    }

    public String getMessage() { return message; }

    @Override
    public TUIScreen accept(EventVisitor visitor) {
        return visitor.visit(this);
    }
}
