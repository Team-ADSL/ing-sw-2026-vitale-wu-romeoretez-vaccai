package org.adsl.client.view.tui.events;

import org.adsl.client.view.events.Event;

import org.adsl.client.view.tui.screens.TUIScreen;

public class NavigateRightEvent extends Event {
    @Override
    public TUIScreen accept(EventVisitor visitor) {
        return visitor.visit(this);
    }
}
