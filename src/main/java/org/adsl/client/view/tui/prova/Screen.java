package org.adsl.client.view.tui.prova;

public interface Screen implement EventVisitor {
    Screen handleEvent(Event event);
}
