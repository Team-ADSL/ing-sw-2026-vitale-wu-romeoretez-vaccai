package org.adsl.client.view.tui.events;

import org.adsl.client.view.tui.screens.TUIScreen;

/** Arrow-up keystroke. Moves the focus or cursor upward on the current screen. */
public class NavigateUpEvent extends InputEvent {
    @Override
    public TUIScreen accept(InputEventVisitor visitor) {
        return visitor.visit(this);
    }
}
