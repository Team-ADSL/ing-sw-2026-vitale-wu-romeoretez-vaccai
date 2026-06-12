package org.adsl.shared.network.requests;

import org.adsl.shared.exceptions.ServerException;

/**
 * Sent by a client to start a game while still in its lobby, once enough
 * players have joined. Triggers the transition from lobby to active gameplay.
 */
public class StartGameRequest extends ClientRequest{
    @Override
    public <T> void accept(RequestVisitor<T> visitor,T context) throws ServerException {
        visitor.visit(this, context);
    }
}