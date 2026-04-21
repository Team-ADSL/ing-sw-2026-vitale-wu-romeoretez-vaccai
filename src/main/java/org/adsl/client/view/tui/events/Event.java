package org.adsl.client.view.tui.events;

import org.adsl.client.view.tui.screens.Screen;

public abstract class Event {
    public abstract Screen accept(EventVisitor visitor);
}
