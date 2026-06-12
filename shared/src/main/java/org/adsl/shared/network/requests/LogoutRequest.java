package org.adsl.shared.network.requests;

import org.adsl.shared.exceptions.ServerException;

/**
 * Sent when a logged-in client requests to log out, ending the session
 * started by {@link LoginRequest} and returning the client to a logged-out state.
 */
public class LogoutRequest extends ClientRequest{
    @Override
    public <T> void accept(RequestVisitor<T> visitor, T context) throws ServerException {
        visitor.visit(this,  context);
    }
}
