package org.adsl.client.view.tui.events;

import org.adsl.client.view.tui.screens.TUIScreen;

public abstract class InputEvent {
    public abstract TUIScreen accept(InputEventVisitor visitor);
}
