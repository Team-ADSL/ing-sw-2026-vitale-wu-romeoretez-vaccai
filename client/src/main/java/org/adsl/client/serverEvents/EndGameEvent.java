package org.adsl.client.serverEvents;

import org.adsl.client.view.Screen;
import org.adsl.shared.model.DBRecord;

import java.util.List;

/**
 * Server event fired when the game ends. Carries the final leaderboard;
 * {@code results} is {@code null} if the game ended before it started
 * (all lobby players left).
 *
 * @param results final leaderboard entries, or {@code null}
 */
public record EndGameEvent(List<DBRecord> results) implements ServerEvent {

    @Override
    public <S extends Screen<S>> S accept(EventVisitor<S> visitor) {
        return visitor.visit(this);
    }
}
