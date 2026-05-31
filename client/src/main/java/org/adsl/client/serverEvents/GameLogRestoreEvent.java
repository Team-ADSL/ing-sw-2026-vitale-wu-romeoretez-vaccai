package org.adsl.client.serverEvents;

import org.adsl.client.view.Screen;

import java.util.List;

/**
 * Server event delivered to a (re)joining client carrying the full game-log
 * transcript so the view can repopulate its local log after a disconnect or a
 * fresh process restart. Handlers replace the local log with this history
 * rather than appending, to avoid duplicates on a same-process reconnect.
 *
 * @param history full ordered list of game-log lines accumulated server-side
 */
public record GameLogRestoreEvent(List<String> history) implements ServerEvent {

    @Override
    public <S extends Screen<S>> S accept(EventVisitor<S> visitor) {
        return visitor.visit(this);
    }
}
