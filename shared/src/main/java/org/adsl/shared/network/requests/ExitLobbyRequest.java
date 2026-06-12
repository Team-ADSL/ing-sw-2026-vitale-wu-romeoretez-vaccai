package org.adsl.shared.network.requests;

import org.adsl.shared.exceptions.ServerException;

/**
 * Sent when a client leaves the lobby of a game they have joined but that
 * has not started yet, before {@link StartGameRequest} is sent.
 */
public class ExitLobbyRequest extends ClientRequest{
    @Override
    public <T> void accept(RequestVisitor<T> visitor, T context) throws ServerException {
        visitor.visit(this, context);
    }
}
