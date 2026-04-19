package org.adsl.utils.fakes;

import org.adsl.server.network.VirtualClient;
import org.adsl.shared.network.responses.ServerResponse;

import java.util.*;

public class FakeVirtualClient extends VirtualClient {
    public boolean loginNeededSent = false;
    public boolean pingSent = false;
    public boolean responseSent = false;
    public boolean connectionClosed = false;
    public List<String> errorsReceived = new ArrayList<>();

    public FakeVirtualClient() { super(null); }

    @Override public void sendPing() { this.pingSent = true; }
    @Override public void sendResponse(ServerResponse response) { responseSent = true; }
    @Override public void closeConnection() { connectionClosed = true; }
    @Override public void sendLoginNeededResponse() { this.loginNeededSent = true; }
    @Override public void sendErrorMessage(String msg) { this.errorsReceived.add(msg); }
    @Override public void handleDisconnection() {setConnected(false); }
}