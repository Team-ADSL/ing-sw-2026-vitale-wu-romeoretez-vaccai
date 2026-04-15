package org.adsl.shared.network.responses;

import org.adsl.client.exceptions.InvalidResponseException;

public class ServerPing extends ServerResponse{
    @Override
    public void accept(ResponseVisitor visitor) throws InvalidResponseException {
        visitor.visit(this);
    }
}
