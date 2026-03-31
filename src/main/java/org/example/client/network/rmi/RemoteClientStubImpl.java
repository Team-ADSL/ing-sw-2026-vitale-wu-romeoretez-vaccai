package org.example.client.network.rmi;

import org.example.client.view.ClientListener;
import org.example.shared.model.DatasourceDTO;
import org.example.shared.model.MatchResult;
import org.example.shared.network.RemoteClientStub;

import java.util.List;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;


public class RemoteClientStubImpl extends UnicastRemoteObject implements RemoteClientStub {
    private final transient ClientListener listener;

    public RemoteClientStubImpl(ClientListener listener) throws RemoteException {
        super();
        this.listener = listener;
    }

    @Override
    public void updateModel(DatasourceDTO data) throws RemoteException {
        if (listener != null) {
            listener.onDataReceived(data);
        }
    }

    @Override
    public void sendErrorMessage(String error) throws RemoteException {
        if (listener != null) {
            listener.onErrorReceived(error);
        }
    }

    @Override
    public void updateMatchResults(List<MatchResult> matchResults) throws RemoteException {
        if (listener != null) {
            listener.onEndGame(matchResults);
        }
    }
}
