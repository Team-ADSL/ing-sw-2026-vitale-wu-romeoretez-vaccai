package org.example.server.network.rmi;

import org.example.server.controller.ServerController;
import org.example.server.model.Datasource;
import org.example.server.network.VirtualClient;
import org.example.shared.model.MatchResult;
import org.example.shared.network.RemoteClientStub;

import java.util.List;

public class RMIClientHandler extends VirtualClient {
    private final RemoteClientStub clientStub;

    public RMIClientHandler(ServerController serverController, RemoteClientStub clientStub) {
        this.clientStub = clientStub;
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
