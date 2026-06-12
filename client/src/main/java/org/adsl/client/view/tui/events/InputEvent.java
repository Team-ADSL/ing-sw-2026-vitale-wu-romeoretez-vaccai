package org.adsl.client.view.tui.events;

import org.adsl.client.view.tui.screens.TUIScreen;

/**
 * Abstract base for all keyboard input events produced by the TUI.
 * Each subtype represents a distinct key or key category (arrow, enter,
 * character, etc.) and is dispatched via {@link InputEventVisitor} to the
 * current {@code TUIScreen}.
 */
public abstract class InputEvent {

    /**
     * Dispatches this event to the matching {@code visit} method on the given visitor.
     *
     * @param visitor the visitor (typically the currently active {@code TUIScreen})
     * @return the screen to display next, or the same screen if nothing changes
     */
    public abstract TUIScreen accept(InputEventVisitor visitor);
}
