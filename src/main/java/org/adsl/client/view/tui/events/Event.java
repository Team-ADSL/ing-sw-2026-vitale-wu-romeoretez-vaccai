package org.adsl.client.view.tui.events;

import org.adsl.client.view.tui.screens.TUIScreen;

public abstract class Event {
    public abstract TUIScreen accept(EventVisitor visitor);
}
