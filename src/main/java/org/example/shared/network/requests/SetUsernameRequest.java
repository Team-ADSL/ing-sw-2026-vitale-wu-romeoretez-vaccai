package org.example.shared.network.requests;

import org.example.shared.exceptions.InvalidRequestException;

public class SetUsernameRequest extends ClientRequest{
    @Override
    public <T> void accept(RequestVisitor<T> visitor, T context) throws InvalidRequestException {
        visitor.visit(this, context);
    }
}
