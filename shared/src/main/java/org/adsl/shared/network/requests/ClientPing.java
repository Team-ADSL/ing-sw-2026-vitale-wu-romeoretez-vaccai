package org.adsl.shared.network.requests;

import org.adsl.shared.exceptions.ServerException;

/**
 * Sent periodically by the client as a heartbeat to let the server know it
 * is still connected and responsive. Carries no data.
 */
public class ClientPing extends ClientRequest {
    @Override
    public <T> void accept(RequestVisitor<T> visitor, T context) throws ServerException {
        visitor.visit(this, context);
    }
}
