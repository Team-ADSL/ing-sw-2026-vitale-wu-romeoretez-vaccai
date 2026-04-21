package org.adsl.client.view.tui.events;

import org.adsl.client.view.tui.screens.Screen;
import org.adsl.shared.model.MatchResult;

import java.util.List;

public class EndGameEvent extends Event {
    private final List<MatchResult> results;

    public EndGameEvent(List<MatchResult> results) {
        this.results = results;
    }

    public List<MatchResult> getResults() { return results; }

    @Override
    public Screen accept(EventVisitor visitor) {
        return visitor.visit(this);
    }
}
