package org.example.shared.network.requests;

import org.example.server.exceptions.GameException;

public class LoginRequest extends ClientRequest{
    private final String username;

    public LoginRequest(String username) {
        this.username = username;
    }

    @Override
    public <T> void accept(RequestVisitor<T> visitor, T context) throws GameException {
        visitor.visit(this, context);
    }

    public String getUsername() {
        return username;
    }
}
