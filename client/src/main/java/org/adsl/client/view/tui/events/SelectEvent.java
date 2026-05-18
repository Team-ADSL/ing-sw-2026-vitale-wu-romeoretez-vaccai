package org.adsl.client.view.tui.events;

import org.adsl.client.view.tui.screens.TUIScreen;

/**
 * SPACE keystroke (or equivalent secondary-select key). Used to toggle
 * selection of an item independently of submitting the whole form.
 */
public class SelectEvent extends InputEvent {
    @Override
    public TUIScreen accept(InputEventVisitor visitor) {
        return visitor.visit(this);
    }
}
