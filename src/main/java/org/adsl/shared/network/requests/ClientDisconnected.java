package org.adsl.shared.network.requests;

import org.adsl.server.exceptions.ServerException;

public class ClientDisconnected extends ClientRequest{
    @Override
    public <T> void accept(RequestVisitor<T> visitor, T context) throws ServerException {
        visitor.visit(this, context);
    }
}
