package org.adsl.shared.network.responses;

import org.adsl.shared.exceptions.InvalidResponseException;

/**
 * Response sent immediately after a client connects (or after logout) to
 * signal that it must authenticate with a {@code LoginRequest} before any
 * other request is accepted.
 */
public class LoginNeeded extends ServerResponse{
    @Override
    public void accept(ResponseVisitor visitor) throws InvalidResponseException {
        visitor.visit(this);
    }
}
