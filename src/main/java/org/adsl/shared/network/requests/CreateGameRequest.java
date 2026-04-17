package org.adsl.shared.network.requests;

import org.adsl.server.exceptions.ServerException;

public class CreateGameRequest extends ClientRequest{
    private final int numPlayer;

    public CreateGameRequest(int numPlayer) {
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
