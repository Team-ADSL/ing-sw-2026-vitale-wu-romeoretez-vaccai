package org.adsl.shared.network.requests;

import org.adsl.server.exceptions.ServerException;

public class EnterGameRequest extends ClientRequest{
    private final int gameId;

    public EnterGameRequest(int gameId) {
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
