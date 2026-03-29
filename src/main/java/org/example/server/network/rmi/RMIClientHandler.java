package org.example.server.network.rmi;

import org.example.server.controller.ServerController;
import org.example.server.network.VirtualClient;
import org.example.shared.model.DatasourceDTO;
import org.example.shared.network.RemoteClientStub;

public class RMIClientHandler extends VirtualClient {
    private final RemoteClientStub clientStub;

    public RMIClientHandler(ServerController serverController, RemoteClientStub clientStub) {
        this.clientStub = clientStub;
        super(serverController);
    }

    @Override
    public void update(DatasourceDTO datasource) {

    }

    @Override
    public void sendErrorMessage(String error) {

    }
}
