package org.adsl.shared.network.requests;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.adsl.shared.exceptions.ServerException;

public class CreateGameRequest extends ClientRequest{
    private final int numPlayer;

    @JsonCreator
    public CreateGameRequest(@JsonProperty("numPlayer")int numPlayer) {
        this.numPlayer = numPlayer;
    }

    @Override
    public <T> void accept(RequestVisitor<T> visitor, T context) throws ServerException {
        visitor.visit(this, context);
    }

    public int getNumPlayer() {
        return numPlayer;
    }
}
