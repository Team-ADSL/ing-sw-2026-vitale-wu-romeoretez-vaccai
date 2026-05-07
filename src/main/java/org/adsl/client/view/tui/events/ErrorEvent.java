package org.adsl.client.view.tui.events;

import org.adsl.client.view.tui.screens.TUIScreen;

public class ErrorEvent extends Event {
    private final String message;

    public ErrorEvent(String message) {
        this.message = message;
    }

    public String getMessage() { return message; }

    @Override
    public TUIScreen accept(EventVisitor visitor) {
        return visitor.visit(this);
    }
}
