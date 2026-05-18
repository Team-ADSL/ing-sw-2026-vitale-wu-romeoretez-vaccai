package org.adsl.shared.network.remote;

import org.adsl.shared.network.responses.ServerResponse;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * RMI callback interface implemented by the client. The server holds a reference
 * to this stub and calls {@link #sendResponse} to push responses to the client
 * without polling.
 */
public interface RemoteClientStub extends Remote {

    /**
     * Delivers a server response to the client.
     *
     * @param response the response to deliver
     * @throws RemoteException if the RMI call fails
     */
    void sendResponse(ServerResponse response) throws RemoteException;
}
