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
    private final ExecutorService threadPool;

    public RemoteServerServiceImpl() throws RemoteException {
        super();
        this.serverController = null;
        this.clients = new ConcurrentHashMap<>();
        this.threadPool = Executors.newCachedThreadPool();
    }

    @Override
    public void connect(RemoteClientStub clientCallback) throws RemoteException {
        System.out.println("New client connected with RMI.");
        RMIClientHandler handler = new RMIClientHandler(serverController, clientCallback, this);
        clients.put(clientCallback, handler);
        threadPool.submit(() -> {
            try { handler.handleConnection(); }
            catch (Exception e) { System.err.println("[RMI] Error in handleConnection: " + e); }
        });
    }

    @Override
    public void sendRequest(ClientRequest request, RemoteClientStub clientStub) throws RemoteException {
        RMIClientHandler handler = clients.get(clientStub);
        if (handler != null) {
            threadPool.submit(() -> {
                try { handler.processRequest(request); }
                catch (Exception e) { System.err.println("[RMI] Error processing request from "
                        + handler.getClientUsername() + ": " + e); }
            });
        } else {
            System.err.println("Unregistered client.");
        }
    }

    @Override
    public void disconnect(RemoteClientStub clientStub) {
        RMIClientHandler handler = clients.get(clientStub);
        if (handler != null) {
            threadPool.submit(() -> {
                try { handler.handleDisconnection(); }
                catch (Exception e) { System.err.println("[RMI] Error in handleDisconnection: " + e); }
            });
        } else {
            System.err.println("Unregistered client.");
        }
    }

    public void remove(RemoteClientStub clientStub){
        RMIClientHandler handler = clients.get(clientStub);
        if (handler != null) {
            clients.remove(clientStub);
        } else {
            System.err.println("Unregistered client.");
        }
    }

    public void setServerController(ServerController serverController) {
        this.serverController = serverController;
    }
}