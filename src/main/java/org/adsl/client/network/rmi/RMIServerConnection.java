package org.adsl.client.network.rmi;

import org.adsl.client.AppCoordinator;
import org.adsl.client.network.ServerConnection;
import org.adsl.shared.network.remote.RemoteServerService;
import org.adsl.shared.network.requests.ClientRequest;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class RMIServerConnection implements ServerConnection {
    private RemoteServerService serverStub;
    private RemoteClientStubImpl clientStub;
    private AppCoordinator appCoordinator;

    @Override
    public void connect(String ip, int port) throws Exception {
        Registry registry = LocateRegistry.getRegistry(ip, port);
        serverStub = (RemoteServerService) registry.lookup("GameServer");
        clientStub = new RemoteClientStubImpl(appCoordinator);
        serverStub.connect(clientStub);
    }

    @Override
    public void sendRequest(ClientRequest request) throws Exception {
        serverStub.sendRequest(request, clientStub);
    }

    @Override
    public void disconnect() throws Exception {
        try {
            if (serverStub != null && clientStub != null) {
                serverStub.disconnect(clientStub);
            }
        } catch (Exception e) {
            System.err.println("Error sending disconnection message to server.");
        }

        try {
            if (clientStub != null) {
                UnicastRemoteObject.unexportObject(clientStub, true);
                System.out.println("Local RMI object removed successfully.");
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }


    @Override
    public void setAppCoordinator(AppCoordinator appCoordinator) {
        this.appCoordinator = appCoordinator;
    }
}