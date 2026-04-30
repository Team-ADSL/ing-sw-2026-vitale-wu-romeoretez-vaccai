package org.adsl.server.network.rmi;

import org.adsl.server.controller.ServerController;
import org.adsl.shared.network.remote.RemoteClientStub;
import org.adsl.shared.network.remote.RemoteServerService;
import org.adsl.shared.network.requests.ClientRequest;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RemoteServerServiceImpl extends UnicastRemoteObject implements RemoteServerService {
    private ServerController serverController;
    private final Map<RemoteClientStub, RMIClientHandler> clients;
    private final Map<RemoteClientStub, ExecutorService> clientExecutors;

    public RemoteServerServiceImpl() throws RemoteException {
        super();
        this.serverController = null;
        this.clients = new ConcurrentHashMap<>();
        this.clientExecutors = new ConcurrentHashMap<>();
    }

    @Override
    public void connect(RemoteClientStub clientCallback) throws RemoteException {
        System.out.println("New client connected with RMI.");
        RMIClientHandler handler = new RMIClientHandler(serverController, clientCallback, this);
        clients.put(clientCallback, handler);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        clientExecutors.put(clientCallback, executor);
        executor.submit(handler::handleConnection);
    }

    @Override
    public void sendRequest(ClientRequest request, RemoteClientStub clientStub) throws RemoteException {
        RMIClientHandler handler = clients.get(clientStub);
        ExecutorService executor = clientExecutors.get(clientStub);
        if (handler != null && executor != null) {
            executor.submit(() -> handler.processRequest(request));
        } else {
            System.err.println("Unregistered client.");
        }
    }

    @Override
    public void disconnect(RemoteClientStub clientStub) {
        RMIClientHandler handler = clients.get(clientStub);
        ExecutorService executor = clientExecutors.get(clientStub);
        if (handler != null && executor != null) {
            executor.submit(handler::handleDisconnection);
        } else {
            System.err.println("Unregistered client.");
        }
    }

    public void remove(RemoteClientStub clientStub){
        RMIClientHandler handler = clients.get(clientStub);
        if (handler != null) {
            clients.remove(clientStub);
            ExecutorService executor = clientExecutors.remove(clientStub);
            if (executor != null) {
                executor.shutdown();
            }
        } else {
            System.err.println("Unregistered client.");
        }
    }

    public void setServerController(ServerController serverController) {
        this.serverController = serverController;
    }
}
