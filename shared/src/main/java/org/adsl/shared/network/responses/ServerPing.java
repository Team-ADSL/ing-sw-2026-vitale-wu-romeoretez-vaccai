package org.adsl.shared.network.responses;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.adsl.shared.exceptions.InvalidResponseException;

/**
 * Keep-alive response sent by the server in reply to a {@code ClientPing}.
 * Overrides {@link #isHeartbeat()} to return {@code true} so the client
 * dispatcher bypasses the visual-pacing queue for this message.
 */
public class ServerPing extends ServerResponse{
    @Override
    public void accept(ResponseVisitor visitor) throws InvalidResponseException {
        visitor.visit(this);
    }

    @Override
    @JsonIgnore
    public boolean isHeartbeat() {
        return true;
    }
}
