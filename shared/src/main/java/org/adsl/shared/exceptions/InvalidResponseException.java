package org.adsl.shared.exceptions;

/**
 * Thrown by {@code ResponseVisitor} implementations on the client side when a
 * received {@code ServerResponse} is not valid in the current client state
 * (e.g. a {@code GameUpdate} arriving while still on the home screen).
 */
public class InvalidResponseException extends RuntimeException {
    public InvalidResponseException(String message) {
        super(message);
    }
}
