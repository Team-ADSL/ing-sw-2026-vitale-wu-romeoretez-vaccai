package org.adsl.shared.network.responses;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.adsl.shared.exceptions.InvalidResponseException;

/**
 * Response sent by the server when a client request is rejected or an internal
 * error occurs. The {@code message} field (inherited from {@code ServerResponse})
 * contains the human-readable error description shown to the user.
 */
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
