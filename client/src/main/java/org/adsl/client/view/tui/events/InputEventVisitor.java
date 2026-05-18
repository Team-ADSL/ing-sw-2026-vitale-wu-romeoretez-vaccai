package org.adsl.client.view.tui.events;

import org.adsl.client.view.tui.screens.TUIScreen;

/**
 * Visitor interface for dispatching TUI keyboard input events to the current
 * screen without instanceof checks. Implemented by {@code TUIScreen} with
 * default no-op handlers; concrete screens override only what they need.
 */
public interface InputEventVisitor {
    TUIScreen visit(ConfirmEvent event);
    TUIScreen visit(SelectEvent event);
    TUIScreen visit(NavigateLeftEvent event);
    TUIScreen visit(NavigateRightEvent event);
    TUIScreen visit(NavigateUpEvent event);
    TUIScreen visit(NavigateDownEvent event);
    TUIScreen visit(CharInputEvent event);
    TUIScreen visit(BackspaceEvent event);
}
