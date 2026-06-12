package org.adsl.client.network.rmi;

import org.adsl.client.AppCoordinator;
import org.adsl.client.network.ServerConnection;
import org.adsl.shared.network.remote.RemoteServerService;
import org.adsl.shared.network.requests.ClientRequest;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * {@code ServerConnection} implementation that communicates with the server
 * via Java RMI. Looks up the {@code "GameServer"} stub from the RMI registry,
 * exports a {@code RemoteClientStubImpl} as the callback object, and forwards
 * all client requests through {@code RemoteServerService.sendRequest()}.
 */
public class RMIServerConnection implements ServerConnection {
    private RemoteServerService serverStub;
    private RemoteClientStubImpl clientStub;
    private AppCoordinator appCoordinator;
    private boolean disconnected = false;

    /**
     * Looks up the {@code "GameServer"} remote object in the RMI registry at
     * {@code ip:port}, exports a {@link RemoteClientStubImpl} as the callback
     * object for incoming responses, and registers it with the server.
     *
     * @param ip   host name or IP address of the RMI registry
     * @param port port the RMI registry is listening on
     * @throws Exception if the registry lookup, stub export, or server-side
     *                    {@code connect} call fails
     */
    @Override
    public void connect(String ip, int port) throws Exception {
        Registry registry = LocateRegistry.getRegistry(ip, port);
        serverStub = (RemoteServerService) registry.lookup("GameServer");
        clientStub = new RemoteClientStubImpl(appCoordinator);
        serverStub.connect(clientStub);
    }

    /**
     * Forwards a client request to the server, passing this client's callback
     * stub so the server knows where to send the corresponding response.
     *
     * @param request the request to send
     * @throws Exception if the remote call fails
     */
    @Override
    public void sendRequest(ClientRequest request) throws Exception {
        serverStub.sendRequest(request, clientStub);
    }

    /**
     * Notifies the server of this client's disconnection and unexports the
     * local callback stub. Safe to call multiple times: only the first call
     * performs any work.
     *
     * @throws Exception never thrown directly; errors during disconnection
     *                    are caught and logged
     */
    @Override
    public synchronized void disconnect() throws Exception {
        // Idempotent: shutdown paths (GUI X-button cleanup + JVM shutdown hook)
        // can both reach here; second call would hit an already-unexported stub
        // and emit a misleading "object not exported" log.
        if (disconnected) return;
        disconnected = true;
        try {
            if (serverStub != null && clientStub != null) {
                serverStub.disconnect(clientStub);
            }
        } catch (Exception e) {
            System.err.println("Error sending disconnection message to server.");
        }

        try {
            if (clientStub != null) {
                UnicastRemoteObject.unexportObject(clientStub, true);
                clientStub.shutdown();
                System.out.println("Local RMI object removed successfully.");
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }


    /**
     * Sets the coordinator that will receive server responses delivered to
     * the RMI callback stub.
     *
     * @param appCoordinator the coordinator to notify of incoming responses
     */
    @Override
    public void setAppCoordinator(AppCoordinator appCoordinator) {
        this.appCoordinator = appCoordinator;
    }
}