package org.example.shared.network.requests;

import org.example.server.exceptions.InvalidRequestException;

public class ClientConnection extends ClientRequest{
    @Override
    public <T> void accept(RequestVisitor<T> visitor, T context) throws InvalidRequestException {
        visitor.visit(this, context);
    }
}
