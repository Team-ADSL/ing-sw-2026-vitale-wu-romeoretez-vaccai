package org.example.shared.network.requests;

import org.example.shared.exceptions.InvalidRequestException;
import org.example.shared.network.RequestVisitor;

public class ClientDisconnected extends ClientRequest{
    public ClientDisconnected(int gameId, String username) {
        super(gameId, username);
    }

    @Override
    public <T> void accept(RequestVisitor<T> visitor, T context) throws InvalidRequestException {
        visitor.visit(this, context);
    }
}
