package org.example.server.network.rmi;

import org.example.server.controller.ServerController;
import org.example.server.model.Datasource;
import org.example.server.network.VirtualClient;
import org.example.shared.model.MatchResult;
import org.example.shared.network.RemoteClientStub;

import java.rmi.RemoteException;
import java.util.List;

public class RMIClientHandler extends VirtualClient {
    private final RemoteClientStub clientStub;

    public RMIClientHandler(ServerController serverController, RemoteClientStub clientStub) {
        super(serverController);
        this.clientStub = clientStub;
    }


    @Override
    public void update(Datasource datasource) {
        try {
            clientStub.updateModel(datasource.createDTO());
        } catch (RemoteException e) {
            handleDisconnection();
        }
    }

    @Override
    public void sendErrorMessage(String error) {
        try {
            clientStub.sendErrorMessage(error);
        } catch (RemoteException e) {
            handleDisconnection();
        }
    }

    @Override
    public void update(int gameId, List<MatchResult> matchResults) {
        try {
            clientStub.updateMatchResults(matchResults);
        } catch (RemoteException e) {
            handleDisconnection();
        }
    }
}
