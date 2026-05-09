package org.adsl.client.serverEvents;

import org.adsl.client.view.Screen;

import java.util.List;

public record LobbyUpdateEvent(int gameId, List<String> players, int numPlayersAllowed) implements ServerEvent {

    @Override
    public <S extends Screen<S>> S accept(EventVisitor<S> visitor) {
        return visitor.visit(this);
    }
}
