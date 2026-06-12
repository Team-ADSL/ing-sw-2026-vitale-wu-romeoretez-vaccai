package org.adsl.shared.network.responses;

import org.adsl.shared.exceptions.InvalidResponseException;

/**
 * Response sent immediately after a client connects (or after logout) to
 * signal that it must authenticate with a {@code LoginRequest} before any
 * other request is accepted.
 */
public class LoginNeeded extends ServerResponse{
    /**
     * Dispatches this response to {@link ResponseVisitor#visit(LoginNeeded)}.
     *
     * @param visitor the visitor that will handle this response
     * @throws InvalidResponseException if the visitor cannot process this response
     */
    @Override
    public void accept(ResponseVisitor visitor) throws InvalidResponseException {
        visitor.visit(this);
    }
}
