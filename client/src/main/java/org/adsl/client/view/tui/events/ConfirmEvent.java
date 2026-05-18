package org.adsl.client.view.tui.events;

import org.adsl.client.view.tui.screens.TUIScreen;

/**
 * ENTER keystroke. Used to confirm a selection or submit a text input on the
 * current TUI screen.
 */
public class ConfirmEvent extends InputEvent {
    @Override
    public TUIScreen accept(InputEventVisitor visitor) {
        return visitor.visit(this);
    }
}
