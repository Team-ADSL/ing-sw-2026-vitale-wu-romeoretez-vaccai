package org.example.shared.network.remote;

import org.example.shared.network.responses.ServerResponse;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteClientStub extends Remote {
    void sendResponse(ServerResponse response) throws RemoteException;
}
