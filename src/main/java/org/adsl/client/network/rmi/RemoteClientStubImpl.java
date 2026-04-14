package org.adsl.client.network.rmi;

import org.adsl.client.AppCoordinator;
import org.adsl.shared.network.remote.RemoteClientStub;
import org.adsl.shared.network.responses.*;

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
