package org.adsl.server.network.socket;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.adsl.server.controller.ServerController;
import org.adsl.server.network.VirtualClient;
import org.adsl.shared.network.JsonMessageHandler;
import org.adsl.shared.network.requests.ClientRequest;
import org.adsl.shared.network.responses.ServerResponse;

import java.io.*;
import java.net.Socket;

/**
 * {@link VirtualClient} implementation for socket-connected clients.
 * <p>
 * Runs on its own thread (submitted to the server thread pool). Reads
 * newline-delimited JSON from the socket input stream via
 * {@link JsonMessageHandler}, deserialises each line into a
 * {@link ClientRequest}, and dispatches it through {@link #processRequest}.
 * Responses are serialised back to JSON and written to the output stream.
 * The thread exits when the connection is closed or an {@link IOException}
 * occurs, triggering {@link #handleDisconnection()}.
 * </p>
 */
public class SocketClientHandler extends VirtualClient implements Runnable {
    private final Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public SocketClientHandler(Socket socket, ServerController serverController) {
        super(serverController);
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            out.flush();
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            handleConnection();

            String jsonLine;
            while ((jsonLine = in.readLine()) != null) {
                try {
                    ClientRequest request = JsonMessageHandler.deserializeClientRequest(jsonLine);
                    processRequest(request);
                } catch (JsonProcessingException e) {
                    System.err.println("[SOCKET] Invalid json received from "
                            + getClientUsername() + ": " + e.getMessage());
                    sendErrorMessage("Invalid Json format");
                } catch (RuntimeException e) {
                    System.err.println("[SOCKET] Error processing request from "
                            + getClientUsername() + ": " + e);
                }
            }
        } catch (IOException e) {
            System.out.println("[SOCKET] Client disconnected or network error: " + e.getMessage());
        } finally {
            handleDisconnection();
        }
    }

    @Override
    public void closeConnection() {
        try {
            socket.close();
            System.out.println("[SOCKET] Connection closed for: " + getClientUsername());
        } catch (IOException e) {
            System.err.println("ERROR: [SOCKET] Error closing connection: " + e.getMessage());
        }
    }

    @Override
    public void sendResponse(ServerResponse response) {
        try {
            String jsonMessage = JsonMessageHandler.serializeServerResponse(response);

            out.println(jsonMessage);

        } catch (JsonProcessingException e) {
            System.err.println("[SOCKET] Error during serialization of response: " + e.getMessage());
        }
    }
}
