package org.example.client.network.rmi;

import org.example.client.AppCoordinator;
import org.example.client.network.ServerConnection;
import org.example.shared.network.remote.RemoteServerService;
import org.example.shared.network.requests.ClientRequest;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;


public class RMIServerConnection implements ServerConnection {
    private RemoteServerService server;
    private RemoteClientStubImpl clientStub;
    private final AppCoordinator middleware;

    public RMIServerConnection(AppCoordinator middleware) {
        this.middleware = middleware;
    }

    @Override
    public void connect(String ip, int port) throws Exception {
        Registry registry = LocateRegistry.getRegistry(ip, port);
        server = (RemoteServerService) registry.lookup("ServerService");
        clientStub = new RemoteClientStubImpl(middleware);
        server.connect(clientStub);
    }

    @Override
    public void sendRequest(ClientRequest request) throws Exception {
        server.sendRequest(request, clientStub);
    }

    @Override
    public void disconnect() throws Exception {
        server.disconnect(clientStub);
    }
}