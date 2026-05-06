package org.adsl.client.network.socket;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.adsl.client.AppCoordinator;
import org.adsl.client.network.ServerConnection;
import org.adsl.shared.network.JsonMessageHandler;
import org.adsl.shared.network.requests.ClientRequest;
import org.adsl.shared.network.responses.ServerDisconnected;
import org.adsl.shared.network.responses.ServerResponse;

import java.io.*;
import java.net.Socket;

public class SocketClientConnection implements ServerConnection {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private AppCoordinator appCoordinator;
    private volatile boolean isRunning;

    @Override
    public void connect(String ip, int port) throws Exception {
        socket = new Socket(ip, port);

        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        isRunning = true;

        new Thread(this::listenToServer).start();
    }

    private void listenToServer() {
        try {
            String jsonLine;
            while (isRunning && !socket.isClosed() && (jsonLine = in.readLine()) != null) {
                try {
                    ServerResponse response = JsonMessageHandler.deserializeServerResponse(jsonLine);
                    if (appCoordinator != null) {
                        appCoordinator.handleServerResponse(response);
                    }
                } catch (JsonProcessingException e) {
                    System.err.println("[CLIENT SOCKET] Received malformed JSON from server: " + e.getMessage());
                } catch (IllegalArgumentException e) {
                    System.err.println("[CLIENT SOCKET] Unsupported message type from server: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            if (isRunning && appCoordinator != null) {
                System.err.println("[CLIENT SOCKET] Connection lost or interrupted: " + e.getMessage());
                ServerResponse disconnection = new ServerDisconnected();
                appCoordinator.handleServerResponse(disconnection);
            }
        } finally {
            try {
                terminateResources();
            } catch(Exception e) {
                System.err.println("[CLIENT SOCKET] Error during resource cleanup: " + e.getMessage());
            }
        }
    }

    @Override
    public void sendRequest(ClientRequest request) throws Exception {
        String jsonRequest = JsonMessageHandler.serializeClientRequest(request);
        out.println(jsonRequest);
    }

    @Override
    public void disconnect() throws Exception {
        isRunning = false;
        terminateResources();
    }

    private void terminateResources() throws Exception {
        if (in != null) in.close();
        if (out != null) out.close();
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    @Override
    public void setAppCoordinator(AppCoordinator appCoordinator) {
        this.appCoordinator = appCoordinator;
    }
}
