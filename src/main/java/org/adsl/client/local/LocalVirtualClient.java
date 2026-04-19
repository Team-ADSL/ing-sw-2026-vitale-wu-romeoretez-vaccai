package org.adsl.client.local;

import org.adsl.server.controller.ServerController;
import org.adsl.server.network.VirtualClient;
import org.adsl.shared.network.responses.ServerResponse;

import java.util.function.Consumer;

/**
 * A VirtualClient that routes server responses directly to a callback,
 * bypassing any network layer. Used in local mode where server and client
 * run in the same process.
 */
public class LocalVirtualClient extends VirtualClient {
    private final Consumer<ServerResponse> responseHandler;

    public LocalVirtualClient(ServerController serverController, Consumer<ServerResponse> responseHandler) {
        super(serverController);
        this.responseHandler = responseHandler;
    }

    @Override
    public void sendResponse(ServerResponse response) {
        responseHandler.accept(response);
    }

    @Override
    public void closeConnection() {
        setConnected(false);
    }
}
