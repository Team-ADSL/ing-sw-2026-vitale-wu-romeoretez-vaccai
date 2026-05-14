package org.adsl.shared.network.requests;

import org.adsl.shared.exceptions.ServerException;

public class ClientConnection extends ClientRequest{
    @Override
    public <T> void accept(RequestVisitor<T> visitor, T context) throws ServerException {
        visitor.visit(this, context);
    }
}
