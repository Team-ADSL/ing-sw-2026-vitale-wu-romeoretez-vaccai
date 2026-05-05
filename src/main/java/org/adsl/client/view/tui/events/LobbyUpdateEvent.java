package org.adsl.client.view.tui.events;

import org.adsl.client.view.tui.screens.Screen;

import java.util.List;

public class LobbyUpdateEvent extends Event {
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
    public Screen accept(EventVisitor visitor) {
        return visitor.visit(this);
    }
}
