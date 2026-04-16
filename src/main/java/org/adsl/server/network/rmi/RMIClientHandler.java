package org.adsl.server.network.rmi;

import org.adsl.server.controller.ServerController;
import org.adsl.server.network.VirtualClient;
import org.adsl.shared.network.remote.RemoteClientStub;
import org.adsl.shared.network.remote.RemoteServerService;
import org.adsl.shared.network.responses.ServerResponse;

import java.rmi.RemoteException;

public class RMIClientHandler extends VirtualClient {
    private final RemoteClientStub clientStub;
    private final RemoteServerServiceImpl serverService;

    public RMIClientHandler(ServerController serverController, RemoteClientStub clientStub,
                            RemoteServerServiceImpl serverService) {
        super(serverController);
        this.clientStub = clientStub;
        this.serverService = serverService;
    }


    @Override
    public void sendResponse(ServerResponse response) {
        try {
            clientStub.sendResponse(response);
        } catch (RemoteException e) {
            handleDisconnection();
        }
    }

    @Override
    public void closeConnection(){
        serverService.remove(clientStub);
    }
}
