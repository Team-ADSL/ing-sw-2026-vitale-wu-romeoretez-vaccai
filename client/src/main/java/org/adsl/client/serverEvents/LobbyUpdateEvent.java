package org.adsl.client.serverEvents;

import org.adsl.client.view.Screen;

import java.util.List;

public record LobbyUpdateEvent(int gameId, List<String> players, int numPlayersAllowed, String message) implements ServerEvent {

    public LobbyUpdateEvent(int gameId, List<String> players, int numPlayersAllowed) {
        this(gameId, players, numPlayersAllowed, null);
    }

    @Override
    public <S extends Screen<S>> S accept(EventVisitor<S> visitor) {
        return visitor.visit(this);
    }
}
