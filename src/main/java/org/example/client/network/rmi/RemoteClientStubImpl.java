package org.example.client.network.rmi;

import org.example.client.AppCoordinator;
import org.example.shared.network.remote.RemoteClientStub;
import org.example.shared.network.responses.*;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;


public class RemoteClientStubImpl extends UnicastRemoteObject implements RemoteClientStub {
    private final transient AppCoordinator coordinator;

    public RemoteClientStubImpl(AppCoordinator coordinator) throws RemoteException {
        super();
        this.coordinator = coordinator;
    }

    public void sendResponse(ServerResponse response){
        response.accept(coordinator);
    }
}
