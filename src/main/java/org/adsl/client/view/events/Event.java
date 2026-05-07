package org.adsl.client.view.events;

import org.adsl.client.view.tui.events.EventVisitor;
import org.adsl.client.view.tui.screens.TUIScreen;

/**
 * Base class for every event consumed by the view layer.
 *
 * <p>Subclasses fall in two families:
 * <ul>
 *   <li>{@link ServerEvent} — produced by server callbacks reaching the client
 *       through {@link org.adsl.client.AppCoordinator}. Both TUI and GUI
 *       react to these.</li>
 *   <li>Input events (in {@code client.view.tui.events}) — produced by
 *       keyboard input in the terminal. Only the TUI handles them; the GUI
 *       receives input directly via JavaFX handlers.</li>
 * </ul>
 *
 * Every concrete event must accept a TUI {@link EventVisitor} returning a
 * {@link TUIScreen}; server events additionally accept a {@code GUIEventVisitor}
 * returning a {@code GUIScreen} (see {@link ServerEvent}).
 */
public abstract class Event {
    public abstract TUIScreen accept(EventVisitor visitor);
}
