package org.adsl.client.view.tui.events;

import org.adsl.client.view.tui.screens.Screen;

public class NavigateDownEvent extends Event {
    @Override
    public Screen accept(EventVisitor visitor) {
        return visitor.visit(this);
    }
}
