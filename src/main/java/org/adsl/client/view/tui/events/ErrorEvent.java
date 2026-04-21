package org.adsl.client.view.tui.events;

import org.adsl.client.view.tui.screens.Screen;

public class ErrorEvent extends Event {
    private final String message;

    public ErrorEvent(String message) {
        this.message = message;
    }

    public String getMessage() { return message; }

    @Override
    public Screen accept(EventVisitor visitor) {
        return visitor.visit(this);
    }
}
