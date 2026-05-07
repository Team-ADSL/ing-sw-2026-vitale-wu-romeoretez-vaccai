package org.adsl.client.view.events;

import org.adsl.client.view.gui.screens.GUIScreen;
import org.adsl.client.view.tui.events.EventVisitor;

import org.adsl.client.view.tui.screens.TUIScreen;

import java.util.List;

public class HomeUpdateEvent extends ServerEvent {
    private final List<Integer> activeGames;

    public HomeUpdateEvent(List<Integer> activeGames) {
        this.activeGames = activeGames;
    }

    public List<Integer> getActiveGames() { return activeGames; }

    @Override
    public TUIScreen accept(EventVisitor visitor) {
        return visitor.visit(this);
    }

    @Override
    public GUIScreen accept(GUIEventVisitor visitor) {
        return visitor.visit(this);
    }
}
