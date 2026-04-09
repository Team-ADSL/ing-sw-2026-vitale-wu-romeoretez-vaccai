package org.example.shared.network.requests;

import org.example.server.exceptions.InvalidRequestException;

public class CreateGameRequest extends ClientRequest{
    private final int numPlayer;

    public CreateGameRequest(int numPlayer) {
        this.numPlayer = numPlayer;
    }

    @Override
    public <T> void accept(RequestVisitor<T> visitor, T context) throws InvalidRequestException {
        visitor.visit(this, context);
    }

    public int getNumPlayer() {
        return numPlayer;
    }
}
