package org.adsl.shared.network.requests;

import org.adsl.shared.exceptions.ServerException;

/**
 * Sent automatically when a client first establishes a connection to the server,
 * before any login. Triggers the server's initial connection handshake, which
 * typically replies with the home/lobby state.
 */
public class ClientConnection extends ClientRequest{
    @Override
    public <T> void accept(RequestVisitor<T> visitor, T context) throws ServerException {
        visitor.visit(this, context);
    }
}
