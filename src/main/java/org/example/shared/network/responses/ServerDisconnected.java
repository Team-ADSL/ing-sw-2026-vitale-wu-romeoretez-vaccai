package org.example.shared.network.responses;

import org.example.client.exceptions.InvalidResponseException;

public class ServerDisconnected extends ServerResponse{
    @Override
    public void accept(ResponseVisitor visitor) throws InvalidResponseException {
        visitor.visit(this);
    }
}
