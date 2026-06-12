package org.adsl.shared.network.requests;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.adsl.shared.exceptions.ServerException;

/**
 * Sent when a client wants to join an existing game lobby identified by its id,
 * for example one shown in a lobby list received via the home/lobby update flow.
 */
public class EnterGameRequest extends ClientRequest{
    private final int gameId;

    /**
     * @param gameId the id of the game lobby to join
     */
    @JsonCreator
    public EnterGameRequest(@JsonProperty("gameId")int gameId) {
        this.gameId = gameId;
    }

    @Override
    public <T> void accept(RequestVisitor<T> visitor, T context) throws ServerException {
        visitor.visit(this, context);
    }

    /**
     * @return the id of the game lobby to join
     */
    public int getGameId() {
        return gameId;
    }
}
