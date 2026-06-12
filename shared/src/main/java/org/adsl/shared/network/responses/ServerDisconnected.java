package org.adsl.shared.network.responses;

import org.adsl.shared.exceptions.InvalidResponseException;

/**
 * Response sent by the server to notify the client that it has been
 * forcefully disconnected (e.g. timeout or shutdown). The client should
 * return to the connection screen on receipt.
 */
public class ServerDisconnected extends ServerResponse{
    /**
     * Dispatches this response to {@link ResponseVisitor#visit(ServerDisconnected)}.
     *
     * @param visitor the visitor that will handle this response
     * @throws InvalidResponseException if the visitor cannot process this response
     */
    @Override
    public void accept(ResponseVisitor visitor) throws InvalidResponseException {
        visitor.visit(this);
    }
}
