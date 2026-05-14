package org.adsl.client.serverEvents;

import org.adsl.client.view.Screen;
import org.adsl.client.view.tui.events.InputEventVisitor;
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
 * Every concrete event must accept a TUI {@link InputEventVisitor} returning a
 * {@link TUIScreen}; server events additionally accept a {@code GUIEventVisitor}
 * returning a {@code GUIScreen} (see {@link ServerEvent}).
 */
public abstract class Event<S extends Screen<S>> {
    public abstract S accept(EventVisitor<S> visitor);
}
