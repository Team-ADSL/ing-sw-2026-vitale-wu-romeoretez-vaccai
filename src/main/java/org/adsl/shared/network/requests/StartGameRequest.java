package org.adsl.shared.network.requests;

import org.adsl.server.exceptions.GameException;

public class StartGameRequest extends ClientRequest{
    @Override
    public <T> void accept(RequestVisitor<T> visitor,T context) throws GameException {
        visitor.visit(this, context);
    }
}
