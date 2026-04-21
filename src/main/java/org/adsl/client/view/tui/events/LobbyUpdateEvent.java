package org.adsl.client.view.tui.events;

import org.adsl.client.view.tui.screens.Screen;

import java.util.List;

public class LobbyUpdateEvent extends Event {
    private final List<String> players;

    public LobbyUpdateEvent(List<String> players) {
        this.players = players;
    }

    public List<String> getPlayers() { return players; }

    @Override
    public Screen accept(EventVisitor visitor) {
        return visitor.visit(this);
    }
}
