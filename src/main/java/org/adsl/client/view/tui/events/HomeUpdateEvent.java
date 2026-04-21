package org.adsl.client.view.tui.events;

import org.adsl.client.view.tui.screens.Screen;

import java.util.List;

public class HomeUpdateEvent extends Event {
    private final List<Integer> activeGames;

    public HomeUpdateEvent(List<Integer> activeGames) {
        this.activeGames = activeGames;
    }

    public List<Integer> getActiveGames() { return activeGames; }

    @Override
    public Screen accept(EventVisitor visitor) {
        return visitor.visit(this);
    }
}
