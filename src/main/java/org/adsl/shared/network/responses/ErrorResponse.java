package org.adsl.shared.network.responses;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.adsl.client.exceptions.InvalidResponseException;

public class ErrorResponse extends ServerResponse {
    private final String message;

    @JsonCreator
    public ErrorResponse(@JsonProperty("message") String message) {
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
