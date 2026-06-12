package org.adsl.shared.network.requests;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.adsl.shared.exceptions.ServerException;

/**
 * Sent when a client wants to create a new game lobby with a given number
 * of players. On success, the server creates the lobby and the client
 * becomes its first member.
 */
public class CreateGameRequest extends ClientRequest{
    private final int numPlayer;

    /**
     * @param numPlayer the number of players the new game should support
     */
    @JsonCreator
    public CreateGameRequest(@JsonProperty("numPlayer")int numPlayer) {
        this.numPlayer = numPlayer;
    }

    @Override
    public <T> void accept(RequestVisitor<T> visitor, T context) throws ServerException {
        visitor.visit(this, context);
    }

    /**
     * @return the requested number of players for the new game
     */
    public int getNumPlayer() {
        return numPlayer;
    }
}
