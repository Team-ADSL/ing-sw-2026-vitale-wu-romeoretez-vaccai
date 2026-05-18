package org.adsl.client.view.tui.events;

import org.adsl.client.view.tui.screens.TUIScreen;

/** Arrow-right keystroke. Moves the focus or cursor to the right on the current screen. */
public class NavigateRightEvent extends InputEvent {
    @Override
    public TUIScreen accept(InputEventVisitor visitor) {
        return visitor.visit(this);
    }
}
