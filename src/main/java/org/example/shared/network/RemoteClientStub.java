package org.example.shared.network;

import org.example.shared.model.DatasourceDTO;
import org.example.shared.model.MatchResult;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface RemoteClientStub extends Remote {
    void updateModel(DatasourceDTO datasourceDTO) throws RemoteException;
    void sendErrorMessage(String error) throws RemoteException;
    void updateMatchResults(List<MatchResult> matchResults) throws RemoteException;
}
