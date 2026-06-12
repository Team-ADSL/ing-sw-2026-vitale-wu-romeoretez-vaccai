package org.adsl.shared.network.requests;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.adsl.shared.exceptions.ServerException;

/**
 * Sent when a client logs in with a chosen username, as part of the login
 * flow handled by the server. On success, the username is associated with
 * the client's connection for the rest of the session.
 */
public class LoginRequest extends ClientRequest{
    private final String username;

    /**
     * @param username the username the client wants to log in with
     */
    @JsonCreator
    public LoginRequest(@JsonProperty("username") String username) {
        this.username = username;
    }

    @Override
    public <T> void accept(RequestVisitor<T> visitor, T context) throws ServerException {
        visitor.visit(this, context);
    }

    /**
     * @return the username the client wants to log in with
     */
    public String getUsername() {
        return username;
    }
}
