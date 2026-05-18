package org.adsl.shared.network.responses;

import org.adsl.shared.exceptions.InvalidResponseException;

/**
 * Response sent by the server to notify the client that it has been
 * forcefully disconnected (e.g. timeout or shutdown). The client should
 * return to the connection screen on receipt.
 */
public class ServerDisconnected extends ServerResponse{
    @Override
    public void accept(ResponseVisitor visitor) throws InvalidResponseException {
        visitor.visit(this);
    }
}
