package org.adsl.client.view.tui.events;

import org.adsl.client.view.tui.screens.TUIScreen;

/**
 * Backspace keystroke. Routed as a dedicated event because text-input screens
 * (e.g. {@link org.adsl.client.view.tui.screens.LoginScreen}) need to delete
 * the previous character while non-text screens just ignore it.
 */
public class BackspaceEvent extends InputEvent {

    @Override
    public TUIScreen accept(InputEventVisitor visitor) {
        return visitor.visit(this);
    }
}
