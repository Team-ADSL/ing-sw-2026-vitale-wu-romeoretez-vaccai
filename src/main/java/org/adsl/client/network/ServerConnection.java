package org.adsl.client.network;

import org.adsl.client.AppCoordinator;
import org.adsl.shared.network.requests.ClientRequest;

public interface ServerConnection {
    void connect(String ip, int port) throws Exception;
    void sendRequest(ClientRequest clientRequest) throws Exception;
    void disconnect() throws Exception;
    void setAppCoordinator(AppCoordinator appCoordinator);
}