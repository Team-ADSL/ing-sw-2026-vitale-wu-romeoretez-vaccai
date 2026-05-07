package org.adsl.client.view.events;

import org.adsl.client.view.gui.screens.GUIScreen;
import org.adsl.client.view.tui.events.EventVisitor;

import org.adsl.client.view.tui.screens.TUIScreen;
import org.adsl.shared.model.MatchResult;

import java.util.List;

public class EndGameEvent extends ServerEvent {
    private final List<MatchResult> results;

    public EndGameEvent(List<MatchResult> results) {
        this.results = results;
    }

    public List<MatchResult> getResults() { return results; }

    @Override
    public TUIScreen accept(EventVisitor visitor) {
        return visitor.visit(this);
    }

    @Override
    public GUIScreen accept(GUIEventVisitor visitor) {
        return visitor.visit(this);
    }
}
