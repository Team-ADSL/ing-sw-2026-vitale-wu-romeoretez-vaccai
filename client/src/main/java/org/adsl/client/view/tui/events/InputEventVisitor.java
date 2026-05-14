package org.adsl.client.view.tui.events;

import org.adsl.client.view.tui.screens.TUIScreen;

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
