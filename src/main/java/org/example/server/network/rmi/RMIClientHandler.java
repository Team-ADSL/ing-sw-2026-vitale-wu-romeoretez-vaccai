package org.example.server.network.rmi;

import org.example.server.network.VirtualClient;
import org.example.shared.model.GameDTO;
import org.example.shared.network.RemoteClientStub;

public class RMIClientHandler implements VirtualClient {
    private final RemoteClientStub clientStub;

    public RMIClientHandler(RemoteClientStub clientStub) {
        this.clientStub = clientStub;
    }

    @Override
    public void update(GameDTO game, String error) {

    }
}
