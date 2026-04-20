package org.adsl.shared.network.remote;

import org.adsl.shared.network.requests.ClientRequest;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteServerService extends Remote {
    void connect(RemoteClientStub clientStub) throws RemoteException;
    void sendRequest(ClientRequest request, RemoteClientStub clientStub) throws RemoteException;
    void disconnect(RemoteClientStub clientStub) throws RemoteException;
}
