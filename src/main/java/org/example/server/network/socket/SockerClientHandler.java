package org.example.server.network.socket;

import org.example.server.controller.ServerController;
import org.example.server.model.Datasource;
import org.example.server.network.VirtualClient;
import org.example.shared.model.MatchResult;

import java.net.Socket;
import java.util.List;

public class SockerClientHandler extends VirtualClient {
    private final Socket socket;
    public SockerClientHandler(ServerController serverController, Socket socket) {
        this.socket = socket;
        super(serverController);
    }

    @Override
    public void update(Datasource datasource) {

    }

    @Override
    public void sendErrorMessage(String error) {

    }

    @Override
    public void update(int gameId, List<MatchResult> matchResults) {

    }
}
