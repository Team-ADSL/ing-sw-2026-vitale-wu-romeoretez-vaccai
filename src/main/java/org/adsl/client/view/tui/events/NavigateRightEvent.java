package org.adsl.client.view.tui.events;

import org.adsl.client.view.tui.screens.TUIScreen;

public class NavigateRightEvent extends InputEvent {
    @Override
    public TUIScreen accept(InputEventVisitor visitor) {
        return visitor.visit(this);
    }
}
