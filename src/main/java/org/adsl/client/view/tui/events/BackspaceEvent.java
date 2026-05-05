package org.adsl.client.view.tui.events;

import org.adsl.client.view.tui.screens.Screen;

/**
 * Backspace keystroke. Routed as a dedicated event because text-input screens
 * (e.g. {@link org.adsl.client.view.tui.screens.LoginScreen}) need to delete
 * the previous character while non-text screens just ignore it.
 */
public class BackspaceEvent extends Event {

    @Override
    public Screen accept(EventVisitor visitor) {
        return visitor.visit(this);
    }
}
