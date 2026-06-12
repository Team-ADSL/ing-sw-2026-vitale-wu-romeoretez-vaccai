package org.adsl.shared.network.responses;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.adsl.shared.exceptions.InvalidResponseException;

/**
 * Keep-alive response sent by the server in reply to a {@code ClientPing}.
 * Overrides {@link #isHeartbeat()} to return {@code true} so the client
 * dispatcher bypasses the visual-pacing queue for this message.
 */
public class ServerPing extends ServerResponse{
    /**
     * Dispatches this response to {@link ResponseVisitor#visit(ServerPing)}.
     *
     * @param visitor the visitor that will handle this response
     * @throws InvalidResponseException if the visitor cannot process this response
     */
    @Override
    public void accept(ResponseVisitor visitor) throws InvalidResponseException {
        visitor.visit(this);
    }

    /**
     * Overridden to return {@code true}, marking this response as a heartbeat
     * so the client dispatcher bypasses the visual-pacing queue for it.
     */
    @Override
    @JsonIgnore
    public boolean isHeartbeat() {
        return true;
    }
}
