package org.example.shared.network.requests;

import org.example.server.exceptions.InvalidRequestException;

public class SetUsernameRequest extends ClientRequest{
    private final String username;

    public SetUsernameRequest(String username) {
        this.username = username;
    }

    @Override
    public <T> void accept(RequestVisitor<T> visitor, T context) throws InvalidRequestException {
        visitor.visit(this, context);
    }

    public String getUsername() {
        return username;
    }
}
