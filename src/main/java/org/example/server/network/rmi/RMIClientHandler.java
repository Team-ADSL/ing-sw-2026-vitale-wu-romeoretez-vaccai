package org.example.server.network.rmi;

import org.example.server.controller.ServerController;
import org.example.server.network.VirtualClient;
import org.example.shared.network.remote.RemoteClientStub;
import org.example.shared.network.responses.ServerResponse;

import java.rmi.RemoteException;

public class RMIClientHandler extends VirtualClient {
    private final RemoteClientStub clientStub;

    public RMIClientHandler(ServerController serverController, RemoteClientStub clientStub) {
        super(serverController);
        this.clientStub = clientStub;
    }


    @Override
    public void sendResponse(ServerResponse response) {
        try {
            clientStub.sendResponse(response);
        } catch (RemoteException e) {
            handleDisconnection();
        }
    }
}
