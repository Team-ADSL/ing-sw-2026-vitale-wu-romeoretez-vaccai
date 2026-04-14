package org.adsl.shared.network.remote;

import org.adsl.shared.network.responses.ServerResponse;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteClientStub extends Remote {
    void sendResponse(ServerResponse response) throws RemoteException;
}
