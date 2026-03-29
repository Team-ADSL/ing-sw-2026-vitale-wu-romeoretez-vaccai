package org.example.server.network.socket;

import org.example.server.controller.ServerController;
import org.example.server.network.VirtualClient;
import org.example.shared.model.DatasourceDTO;

import java.net.Socket;

public class SockerClientHandler extends VirtualClient {
    private final Socket socket;
    public SockerClientHandler(ServerController serverController, Socket socket) {
        this.socket = socket;
        super(serverController);
    }

    @Override
    public void update(DatasourceDTO datasource) {

    }

    @Override
    public void sendErrorMessage(String error) {

    }
}
