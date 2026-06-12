package org.adsl.server.network.rmi;

import org.adsl.server.controller.ServerController;
import org.adsl.server.network.VirtualClient;
import org.adsl.shared.network.remote.RemoteClientStub;
import org.adsl.shared.network.remote.RemoteServerService;
import org.adsl.shared.network.responses.ServerResponse;

import java.rmi.RemoteException;

/**
 * {@link VirtualClient} implementation for RMI-connected clients.
 * <p>
 * Pushes responses via the {@link RemoteClientStub} callback reference held by
 * this handler. On {@link RemoteException} (client unreachable) it triggers
 * {@link #handleDisconnection()} to clean up server state. Unregistration from
 * {@link RemoteServerServiceImpl} is performed by {@link #closeConnection()}.
 * </p>
 */
public class RMIClientHandler extends VirtualClient {
    private final RemoteClientStub clientStub;
    private final RemoteServerServiceImpl serverService;

    /**
     * Creates a handler for an RMI-connected client.
     *
     * @param serverController controller used to dispatch requests from this client
     * @param clientStub       remote callback used to push responses to the client
     * @param serverService    RMI endpoint that owns the client registry, used to
     *                          unregister this client on disconnection
     */
    public RMIClientHandler(ServerController serverController, RemoteClientStub clientStub,
                            RemoteServerServiceImpl serverService) {
        super(serverController);
        this.clientStub = clientStub;
        this.serverService = serverService;
    }


    @Override
    public void sendResponse(ServerResponse response) {
        try {
            clientStub.sendResponse(response);
        } catch (RemoteException e) {
            System.err.println("ERROR: [RMI] Failed to send response to: " + getClientUsername() + " - " + e.getMessage());
            handleDisconnection();
        }
    }

    @Override
    public void closeConnection(){
        serverService.remove(clientStub);
        System.out.println("[RMI] Connection closed for: " + getClientUsername());
    }
}
