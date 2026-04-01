package org.example.shared.network.responses;

import org.example.shared.exceptions.InvalidResponseException;

public class SetUsername extends ServerResponse{
    @Override
    public void accept(ResponseVisitor visitor) throws InvalidResponseException {
        visitor.visit(this);
    }
}
