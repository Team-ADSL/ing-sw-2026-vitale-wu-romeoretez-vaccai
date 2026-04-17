package org.adsl.shared.network.requests;

import org.adsl.server.exceptions.ServerException;

public class LoginRequest extends ClientRequest{
    private final String username;

    public LoginRequest(String username) {
        this.username = username;
    }

    @Override
    public <T> void accept(RequestVisitor<T> visitor, T context) throws ServerException {
        visitor.visit(this, context);
    }

    public String getUsername() {
        return username;
    }
}
