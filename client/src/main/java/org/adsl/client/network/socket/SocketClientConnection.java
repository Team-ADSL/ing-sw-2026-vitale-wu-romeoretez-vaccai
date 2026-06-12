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

/**
 * {@code ServerConnection} implementation that communicates with the server
 * via a TCP socket using newline-delimited JSON (same wire format as the
 * server-side {@code SocketClientHandler}). A dedicated daemon listener
 * thread reads lines from the socket and hands them to {@code AppCoordinator}
 * via {@code JsonMessageHandler}. On connection loss it synthesises a
 * {@code ServerDisconnected} response to notify the UI.
 */
public class SocketClientConnection implements ServerConnection {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private AppCoordinator appCoordinator;
    private volatile boolean isRunning;

    /**
     * Opens a TCP socket to {@code ip:port} and starts a daemon listener
     * thread that reads newline-delimited JSON responses from the server.
     *
     * @param ip   server host name or IP address
     * @param port server port to connect to
     * @throws Exception if the socket cannot be opened
     */
    @Override
    public void connect(String ip, int port) throws Exception {
        socket = new Socket(ip, port);

        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        isRunning = true;

        Thread listener = new Thread(this::listenToServer, "socket-listener");
        listener.setDaemon(true);
        listener.start();
    }

    private void listenToServer() {
        boolean serverGone = false;
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
            // Loop ended without exception: readLine returned null (graceful
            // FIN from server). If we didn't initiate the close, treat as
            // unexpected disconnection. Without this the UI would only
            // notice after the ping timeout, losing parity with RMI.
            serverGone = isRunning;
        } catch (Exception e) {
            if (isRunning) {
                System.err.println("[CLIENT SOCKET] Connection lost or interrupted: " + e.getMessage());
                serverGone = true;
            }
        } finally {
            if (serverGone && appCoordinator != null) {
                appCoordinator.handleServerResponse(new ServerDisconnected());
            }
            try {
                terminateResources();
            } catch(Exception e) {
                System.err.println("[CLIENT SOCKET] Error during resource cleanup: " + e.getMessage());
            }
        }
    }

    /**
     * Serializes a request to JSON and writes it as a single line to the
     * server socket.
     *
     * @param request the request to send
     * @throws Exception if serialization or writing to the socket fails
     */
    @Override
    public void sendRequest(ClientRequest request) throws Exception {
        String jsonRequest = JsonMessageHandler.serializeClientRequest(request);
        out.println(jsonRequest);
    }

    /**
     * Stops the listener thread and closes the socket and its streams.
     *
     * @throws Exception if closing the underlying resources fails
     */
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

    /**
     * Sets the coordinator that will receive responses read from the socket.
     *
     * @param appCoordinator the coordinator to notify of incoming responses
     */
    @Override
    public void setAppCoordinator(AppCoordinator appCoordinator) {
        this.appCoordinator = appCoordinator;
    }
}
