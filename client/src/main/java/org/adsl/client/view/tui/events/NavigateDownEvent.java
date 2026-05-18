package org.adsl.client.view.tui.events;

import org.adsl.client.view.tui.screens.TUIScreen;

/** Arrow-down keystroke. Moves the focus or cursor downward on the current screen. */
public class NavigateDownEvent extends InputEvent {
    @Override
    public TUIScreen accept(InputEventVisitor visitor) {
        return visitor.visit(this);
    }
}
