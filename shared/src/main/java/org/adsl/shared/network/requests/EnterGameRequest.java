package org.adsl.shared.network.requests;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.adsl.shared.exceptions.ServerException;

public class EnterGameRequest extends ClientRequest{
    private final int gameId;

    @JsonCreator
    public EnterGameRequest(@JsonProperty("gameId")int gameId) {
        this.gameId = gameId;
    }

    @Override
    public <T> void accept(RequestVisitor<T> visitor, T context) throws ServerException {
        visitor.visit(this, context);
    }

    public int getGameId() {
        return gameId;
    }
}
