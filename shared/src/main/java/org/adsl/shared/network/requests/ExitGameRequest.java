package org.adsl.shared.network.requests;

import org.adsl.shared.exceptions.ServerException;

/**
 * Sent when a client chooses to leave a game that is currently in progress
 * (or has ended), returning the client to the home/lobby state.
 */
public class ExitGameRequest extends ClientRequest{
    @Override
    public <T> void accept(RequestVisitor<T> visitor, T context) throws ServerException {
        visitor.visit(this, context);
    }
}
