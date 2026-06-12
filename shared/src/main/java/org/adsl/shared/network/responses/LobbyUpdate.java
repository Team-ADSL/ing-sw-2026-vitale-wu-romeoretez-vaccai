package org.adsl.shared.network.responses;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.adsl.shared.exceptions.InvalidResponseException;

import java.util.List;

/**
 * Response sent when the lobby roster changes. Carries the {@code gameId}
 * identifying the lobby/game this update concerns, the current list of
 * player names, and the required player count so the client can render the
 * lobby wait screen correctly.
 */
public class LobbyUpdate extends ServerResponse{
    private final int gameId;
    private final List<String> players;
    private final int numPlayerAllowed;

    /**
     * Creates a response describing the current state of a lobby.
     *
     * @param gameId           identifier of the lobby/game this update concerns
     * @param players          current list of player names in the lobby
     * @param numPlayerAllowed number of players required to start the game
     */
    @JsonCreator
    public LobbyUpdate(@JsonProperty("gameId") int gameId,
                       @JsonProperty("players") List<String> players,
                       @JsonProperty("numPlayerAllowed") int numPlayerAllowed) {
        this.gameId = gameId;
        this.players = players;
        this.numPlayerAllowed = numPlayerAllowed;
    }

    /**
     * Dispatches this response to {@link ResponseVisitor#visit(LobbyUpdate)}.
     *
     * @param visitor the visitor that will handle this response
     * @throws InvalidResponseException if the visitor cannot process this response
     */
    @Override
    public void accept(ResponseVisitor visitor) throws InvalidResponseException {
        visitor.visit(this);
    }

    /**
     * Returns the identifier of the lobby/game this update concerns.
     *
     * @return the game ID
     */
    public int getGameId() {
        return gameId;
    }

    /**
     * Returns the current list of player names in the lobby.
     *
     * @return the player names
     */
    public List<String> getPlayers() {
        return players;
    }

    /**
     * Returns the number of players required to start the game.
     *
     * @return the required player count
     */
    public int getNumPlayerAllowed() {
        return numPlayerAllowed;
    }
}
