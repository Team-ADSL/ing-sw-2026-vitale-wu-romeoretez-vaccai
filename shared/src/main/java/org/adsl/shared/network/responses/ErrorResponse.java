package org.adsl.shared.network.responses;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.adsl.shared.exceptions.InvalidResponseException;

public class ErrorResponse extends ServerResponse {

    @JsonCreator
    public ErrorResponse(@JsonProperty("message") String message) {
        super(message);
    }

    @Override
    public void accept(ResponseVisitor visitor) throws InvalidResponseException {
        visitor.visit(this);
    }
}
