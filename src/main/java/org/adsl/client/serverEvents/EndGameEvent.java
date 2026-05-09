package org.adsl.client.serverEvents;

import org.adsl.client.view.Screen;
import org.adsl.shared.model.MatchResult;

import java.util.List;

public record EndGameEvent(List<MatchResult> results) implements ServerEvent {

    @Override
    public <S extends Screen<S>> S accept(EventVisitor<S> visitor) {
        return visitor.visit(this);
    }
}
