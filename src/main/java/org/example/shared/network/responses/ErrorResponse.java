package org.example.shared.network.responses;

import org.example.shared.exceptions.InvalidResponseException;

public class ErrorResponse extends ServerResponse {
    private final String message;

    public ErrorResponse(String message) {
        this.message = message;
    }

    @Override
    public void accept(ResponseVisitor visitor) throws InvalidResponseException {
        visitor.visit(this);
    }

    public String getMessage() {
        return message;
    }
}
