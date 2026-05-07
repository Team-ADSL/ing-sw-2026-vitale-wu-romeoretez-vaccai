package org.adsl.client.view.events;

import org.adsl.client.view.gui.screens.GUIScreen;
import org.adsl.client.view.tui.events.EventVisitor;

import org.adsl.client.view.tui.screens.TUIScreen;

public class LoginNeededEvent extends ServerEvent {
    @Override
    public TUIScreen accept(EventVisitor visitor) {
        return visitor.visit(this);
    }

    @Override
    public GUIScreen accept(GUIEventVisitor visitor) {
        return visitor.visit(this);
    }
}
