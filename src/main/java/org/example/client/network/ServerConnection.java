package org.example.client.network;

import org.example.client.AppCoordinator;
import org.example.shared.network.requests.ClientRequest;

public interface ServerConnection {
    void connect(String ip, int port) throws Exception;
    void sendRequest(ClientRequest clientRequest) throws Exception;
    void disconnect() throws Exception;
    void setAppCoordinator(AppCoordinator appCoordinator);
}