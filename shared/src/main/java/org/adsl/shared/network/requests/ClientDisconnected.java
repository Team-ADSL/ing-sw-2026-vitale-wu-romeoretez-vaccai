package org.adsl.shared.network.requests;

import org.adsl.shared.exceptions.ServerException;

/**
 * Generated server-side when a client's connection is lost or closed
 * (e.g. socket disconnect or RMI failure). Lets the server clean up the
 * disconnected client's state, such as removing them from a lobby or game.
 */
public class ClientDisconnected extends ClientRequest{
    @Override
    public <T> void accept(RequestVisitor<T> visitor, T context) throws ServerException {
        visitor.visit(this, context);
    }
}
