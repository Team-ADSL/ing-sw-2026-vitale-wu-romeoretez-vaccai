package org.adsl.client.serverEvents;

import org.adsl.client.view.Screen;
import org.adsl.shared.model.DBRecord;
import org.adsl.shared.model.MatchResult;

import java.util.List;

/**
 * Server event fired when the game ends. Carries both the single-match
 * standings ({@code results}) and the cumulative DB leaderboard
 * ({@code records}), plus the server-side log {@code message} (used as the
 * alert text when the DB is unavailable). Either list may be {@code null}
 * when the game ended before it started.
 *
 * @param results single-match standings, or {@code null}
 * @param records DB leaderboard, or {@code null} / empty if DB is unavailable
 * @param message server log message, or {@code null}
 */
public record EndGameEvent(List<MatchResult> results, List<DBRecord> records, String message)
        implements ServerEvent {

    @Override
    public <S extends Screen<S>> S accept(EventVisitor<S> visitor) {
        return visitor.visit(this);
    }
}
