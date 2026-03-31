package org.example.shared.network.requests;

import org.example.shared.exceptions.InvalidRequestException;
import org.example.shared.network.RequestVisitor;

public class CreateGameRequest extends ClientRequest{
    public CreateGameRequest(int gameId) {
        super(gameId);
    }

    @Override
    public <T> void accept(RequestVisitor<T> visitor, T context) throws InvalidRequestException {
        visitor.visit(this, context);
    }
}
