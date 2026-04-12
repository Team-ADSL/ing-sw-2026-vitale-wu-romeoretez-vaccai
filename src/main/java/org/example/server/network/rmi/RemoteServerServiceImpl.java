package org.example.server.network.rmi;

import org.example.server.controller.ServerController;
import org.example.shared.network.remote.RemoteClientStub;
import org.example.shared.network.remote.RemoteServerService;
import org.example.shared.network.requests.ClientRequest;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RemoteServerServiceImpl extends UnicastRemoteObject implements RemoteServerService {
    private final ServerController serverController;
    private final Map<RemoteClientStub, RMIClientHandler> clients;

    public RemoteServerServiceImpl(ServerController serverController) throws RemoteException {
        super();
        this.serverController = serverController;
        this.clients = new ConcurrentHashMap<>();
    }

    @Override
    public void connect(RemoteClientStub clientCallback) throws RemoteException {
        System.out.println("New client connected with RMI.");
        RMIClientHandler handler = new RMIClientHandler(serverController, clientCallback);
        clients.put(clientCallback, handler);
        handler.handleConnection();
    }

    @Override
    public void sendRequest(ClientRequest request, RemoteClientStub clientStub) throws RemoteException {
        RMIClientHandler handler = clients.get(clientStub);
        if (handler != null) {
            handler.processRequest(request);
        } else {
            System.err.println("Unregistered client.");
        }
    }

    @Override
    public void disconnect(RemoteClientStub clientStub) {
        RMIClientHandler handler = clients.get(clientStub);
        if (handler != null) {
            handler.handleDisconnection();
            clients.remove(clientStub);
        } else {
            System.err.println("Unregistered client.");
        }
    }
}
