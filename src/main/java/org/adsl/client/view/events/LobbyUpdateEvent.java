package org.adsl.client.view.events;

import org.adsl.client.view.gui.screens.GUIScreen;
import org.adsl.client.view.tui.events.EventVisitor;

import org.adsl.client.view.tui.screens.TUIScreen;

import java.util.List;

public class LobbyUpdateEvent extends ServerEvent {
    private final int gameId;
    private final List<String> players;
    private final int numPlayersAllowed;

    public LobbyUpdateEvent(int gameId, List<String> players, int numPlayersAllowed) {
        this.gameId = gameId;
        this.players = players;
        this.numPlayersAllowed = numPlayersAllowed;
    }

    public int getGameId() { return gameId; }

    public List<String> getPlayers() { return players; }

    public int getNumPlayersAllowed() {
        return numPlayersAllowed;
    }

    @Override
    public TUIScreen accept(EventVisitor visitor) {
        return visitor.visit(this);
    }

    @Override
    public GUIScreen accept(GUIEventVisitor visitor) {
        return visitor.visit(this);
    }
}
