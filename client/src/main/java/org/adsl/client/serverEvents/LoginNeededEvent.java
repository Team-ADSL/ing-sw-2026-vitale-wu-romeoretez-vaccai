package org.adsl.client.serverEvents;

import org.adsl.client.view.Screen;

/**
 * Server event fired immediately after the client connects (or after logout)
 * to signal that the user must enter a username before any other action.
 * Routes to the login screen in both TUI and GUI.
 */
public class LoginNeededEvent implements ServerEvent {
    @Override
    public <S extends Screen<S>> S accept(EventVisitor<S> visitor) {
        return visitor.visit(this);
    }
}
