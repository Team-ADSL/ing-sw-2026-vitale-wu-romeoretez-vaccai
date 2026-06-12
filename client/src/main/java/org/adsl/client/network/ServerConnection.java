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
    /**
     * Establishes the connection to the server and starts any background
     * listener needed to receive responses.
     *
     * @param ip   server host name or IP address
     * @param port server port to connect to
     * @throws Exception if the connection cannot be established
     */
    void connect(String ip, int port) throws Exception;

    /**
     * Sends a request to the server over this connection.
     *
     * @param clientRequest the request to send
     * @throws Exception if the request cannot be sent
     */
    void sendRequest(ClientRequest clientRequest) throws Exception;

    /**
     * Closes the connection and releases any associated resources.
     *
     * @throws Exception if an error occurs while closing the connection
     */
    void disconnect() throws Exception;

    /**
     * Sets the coordinator that receives and dispatches server responses
     * delivered over this connection.
     *
     * @param appCoordinator the coordinator to notify of incoming responses
     */
    void setAppCoordinator(AppCoordinator appCoordinator);
}