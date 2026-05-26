package org.adsl.client.serverEvents;

import org.adsl.client.view.Screen;
import org.adsl.client.view.gui.screens.GUIScreen;

/**
 * Server-derived events handled by both view layers. A server event accepts a
 * {@link EventVisitor} returning a {@link GUIScreen}.
 */
public interface ServerEvent {
    <S extends Screen<S>> S accept(EventVisitor<S> visitor);
}
