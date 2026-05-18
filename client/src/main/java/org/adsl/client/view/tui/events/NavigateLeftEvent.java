package org.adsl.client.view.tui.events;

import org.adsl.client.view.tui.screens.TUIScreen;

/** Arrow-left keystroke. Moves the focus or cursor to the left on the current screen. */
public class NavigateLeftEvent extends InputEvent {
    @Override
    public TUIScreen accept(InputEventVisitor visitor) {
        return visitor.visit(this);
    }
}
