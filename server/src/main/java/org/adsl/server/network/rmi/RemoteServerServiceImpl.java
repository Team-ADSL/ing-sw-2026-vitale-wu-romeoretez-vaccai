package org.adsl.server.network.rmi;

import org.adsl.server.controller.ServerController;
import org.adsl.shared.network.remote.RemoteClientStub;
import org.adsl.shared.network.remote.RemoteServerService;
import org.adsl.shared.network.requests.ClientRequest;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * RMI server-side endpoint. Exported as a {@link UnicastRemoteObject} and bound
 * in the RMI registry under the name {@code "GameServer"}.
 * <p>
 * Each incoming RMI call ({@link #connect}, {@link #sendRequest},
 * {@link #disconnect}) is dispatched on a shared cached thread pool so that
 * RMI worker threads are not blocked by game logic.
 * </p>
 * <p>
 * <b>Ordering note:</b> RMI requests from the same client are submitted to the
 * shared pool sequentially (one submit per call), which preserves arrival order
 * for a single client stub. Cross-client ordering is not guaranteed.
 * </p>
 */
public class RemoteServerServiceImpl extends UnicastRemoteObject implements RemoteServerService {
    private ServerController serverController;
    private final Map<RemoteClientStub, RMIClientHandler> clients;
    private final ExecutorService threadPool;

    /**
     * Exports this object as an RMI remote object. The server controller must
     * be set afterwards via {@link #setServerController}.
     *
     * @throws RemoteException if the RMI export fails
     */
    public RemoteServerServiceImpl() throws RemoteException {
        super();
        this.serverController = null;
        this.clients = new ConcurrentHashMap<>();
        this.threadPool = Executors.newCachedThreadPool();
    }

    /**
     * Registers a new RMI client, creating a {@link RMIClientHandler} for it and
     * asynchronously running {@link RMIClientHandler#handleConnection()} on the
     * shared thread pool.
     *
     * @param clientCallback remote stub used to push responses to this client
     * @throws RemoteException if the RMI call fails
     */
    @Override
    public void connect(RemoteClientStub clientCallback) throws RemoteException {
        System.out.println("New client connected with RMI.");
        RMIClientHandler handler = new RMIClientHandler(serverController, clientCallback, this);
        clients.put(clientCallback, handler);
        threadPool.submit(() -> {
            try { handler.handleConnection(); }
            catch (Exception e) { System.err.println("[RMI] Error in handleConnection: " + e); }
        });
    }

    /**
     * Forwards a request from an already-registered client to its
     * {@link RMIClientHandler}, processed asynchronously on the shared thread pool.
     * If the client is not registered, the request is dropped and an error is logged.
     *
     * @param request    the request sent by the client
     * @param clientStub remote stub identifying the sending client
     * @throws RemoteException if the RMI call fails
     */
    @Override
    public void sendRequest(ClientRequest request, RemoteClientStub clientStub) throws RemoteException {
        RMIClientHandler handler = clients.get(clientStub);
        if (handler != null) {
            threadPool.submit(() -> {
                try { handler.processRequest(request); }
                catch (Exception e) { System.err.println("[RMI] Error processing request from "
                        + handler.getClientUsername() + ": " + e); }
            });
        } else {
            System.err.println("Unregistered client.");
        }
    }

    /**
     * Notifies the {@link RMIClientHandler} for the given client that it has
     * disconnected, processed asynchronously on the shared thread pool.
     * If the client is not registered, an error is logged.
     *
     * @param clientStub remote stub identifying the disconnecting client
     */
    @Override
    public void disconnect(RemoteClientStub clientStub) {
        RMIClientHandler handler = clients.get(clientStub);
        if (handler != null) {
            threadPool.submit(() -> {
                try { handler.handleDisconnection(); }
                catch (Exception e) { System.err.println("[RMI] Error in handleDisconnection: " + e); }
            });
        } else {
            System.err.println("Unregistered client.");
        }
    }

    /**
     * Unregisters a client, removing its {@link RMIClientHandler} from the
     * registry. Called by {@link RMIClientHandler#closeConnection()}.
     * If the client is not registered, an error is logged.
     *
     * @param clientStub remote stub identifying the client to remove
     */
    public void remove(RemoteClientStub clientStub){
        RMIClientHandler handler = clients.get(clientStub);
        if (handler != null) {
            clients.remove(clientStub);
        } else {
            System.err.println("Unregistered client.");
        }
    }

    /**
     * Sets the controller used to dispatch requests from clients connected via RMI.
     *
     * @param serverController the server controller instance
     */
    public void setServerController(ServerController serverController) {
        this.serverController = serverController;
    }
}