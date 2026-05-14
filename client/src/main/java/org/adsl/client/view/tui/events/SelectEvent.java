package org.adsl.client.view.tui.events;

import org.adsl.client.view.tui.screens.TUIScreen;

public class SelectEvent extends InputEvent {
    @Override
    public TUIScreen accept(InputEventVisitor visitor) {
        return visitor.visit(this);
    }
}
