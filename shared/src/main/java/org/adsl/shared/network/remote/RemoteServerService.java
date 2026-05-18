package org.adsl.shared.network.remote;

import org.adsl.shared.network.requests.ClientRequest;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * RMI server-side interface bound in the registry under the name {@code "GameServer"}.
 * Clients look it up, call {@link #connect} to register their callback stub, then
 * send requests via {@link #sendRequest}.
 */
public interface RemoteServerService extends Remote {

    /**
     * Registers a new client and triggers the initial connection handshake.
     *
     * @param clientStub the client's callback stub
     * @throws RemoteException if the RMI call fails
     */
    void connect(RemoteClientStub clientStub) throws RemoteException;

    /**
     * Sends a client request to the server for processing.
     *
     * @param request    the request to process
     * @param clientStub the stub identifying the sending client
     * @throws RemoteException if the RMI call fails
     */
    void sendRequest(ClientRequest request, RemoteClientStub clientStub) throws RemoteException;

    /**
     * Notifies the server that the client is disconnecting gracefully.
     *
     * @param clientStub the stub identifying the disconnecting client
     * @throws RemoteException if the RMI call fails
     */
    void disconnect(RemoteClientStub clientStub) throws RemoteException;
}
