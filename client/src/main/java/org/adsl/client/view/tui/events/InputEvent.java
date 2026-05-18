package org.adsl.client.view.tui.events;

import org.adsl.client.view.tui.screens.TUIScreen;

/**
 * Abstract base for all keyboard input events produced by the TUI.
 * Each subtype represents a distinct key or key category (arrow, enter,
 * character, etc.) and is dispatched via {@link InputEventVisitor} to the
 * current {@code TUIScreen}.
 */
public abstract class InputEvent {
    public abstract TUIScreen accept(InputEventVisitor visitor);
}
