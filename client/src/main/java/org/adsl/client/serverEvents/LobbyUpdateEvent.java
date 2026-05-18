package org.adsl.client.serverEvents;

import org.adsl.client.view.Screen;

import java.util.List;

/**
 * Server event fired when the lobby roster changes (player joined or left).
 *
 * @param gameId            the game identifier
 * @param players           current list of player names in the lobby
 * @param numPlayersAllowed maximum players required to start
 * @param message           optional log message, or {@code null}
 */
public record LobbyUpdateEvent(int gameId, List<String> players, int numPlayersAllowed, String message) implements ServerEvent {

    public LobbyUpdateEvent(int gameId, List<String> players, int numPlayersAllowed) {
        this(gameId, players, numPlayersAllowed, null);
    }

    @Override
    public <S extends Screen<S>> S accept(EventVisitor<S> visitor) {
        return visitor.visit(this);
    }
}
