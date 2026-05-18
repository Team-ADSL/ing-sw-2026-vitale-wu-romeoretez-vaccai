package org.adsl.client.serverEvents;

import org.adsl.client.view.Screen;

/**
 * Server event fired when the server rejects a request or reports an internal
 * error. The current screen keeps its state but stores the message to display
 * an inline error to the user.
 *
 * @param message the error description from the server
 */
public record ErrorEvent(String message) implements ServerEvent {

    @Override
    public <S extends Screen<S>> S accept(EventVisitor<S> visitor) {
        return visitor.visit(this);
    }
}
