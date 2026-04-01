package org.example.shared.network.requests;

import org.example.shared.exceptions.InvalidRequestException;

public class StartGameRequest extends ClientRequest{
    private final int gameId;

    public StartGameRequest(int gameId) {
        this.gameId = gameId;
    }

    @Override
    public <T> void accept(RequestVisitor<T> visitor,T context) throws InvalidRequestException {
        visitor.visit(this, context);
    }

    public int getGameId() {
        return gameId;
    }
}
