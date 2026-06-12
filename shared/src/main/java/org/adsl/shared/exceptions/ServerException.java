package org.adsl.shared.exceptions;

/**
 * Unchecked exception thrown by server-side game logic to signal a request
 * that cannot be fulfilled (invalid state, wrong player, bad input). Caught by
 * {@code ServerController.handleClientRequest} and forwarded to the client as
 * an {@code ErrorResponse}.
 */
public class ServerException extends RuntimeException {
    /**
     * Creates the exception with a message describing the invalid request.
     *
     * @param message description of why the request cannot be fulfilled
     */
    public ServerException(String message) {
        super(message);
    }
}
