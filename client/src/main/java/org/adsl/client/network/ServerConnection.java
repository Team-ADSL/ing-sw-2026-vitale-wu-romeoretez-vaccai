package org.adsl.client.network;

import org.adsl.client.AppCoordinator;
import org.adsl.shared.network.requests.ClientRequest;

/**
 * Abstraction over the client-side network transport. Implemented by
 * {@code SocketClientConnection} (TCP/JSON) and {@code RMIServerConnection}
 * (Java RMI). {@code FakeServerConnection} provides an in-process stub for
 * local testing without a real server.
 */
public interface ServerConnection {
    void connect(String ip, int port) throws Exception;
    void sendRequest(ClientRequest clientRequest) throws Exception;
    void disconnect() throws Exception;
    void setAppCoordinator(AppCoordinator appCoordinator);
}