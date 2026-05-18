package org.adsl.client.serverEvents;

import org.adsl.client.view.Screen;

/**
 * Server event fired once per resolved end-of-round event, carrying both the
 * short title for the UI overlay and the longer per-player delta summary for
 * the game log.
 *
 * @param eventTitle short label shown as an overlay (e.g. {@code "HUNT I"})
 * @param logMessage full per-player food/PP delta summary for the game log
 */
public record EventsTriggeredEvent(String eventTitle, String logMessage) implements ServerEvent {

    @Override
    public <S extends Screen<S>> S accept(EventVisitor<S> visitor) {
        return visitor.visit(this);
    }
}
