package org.adsl.server.network.socket;

import org.adsl.server.controller.ServerController;
import org.adsl.server.network.VirtualClient;
import org.adsl.shared.network.requests.ClientRequest;
import org.adsl.shared.network.responses.ServerResponse;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class SocketClientHandler extends VirtualClient implements Runnable {
    private final Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    public SocketClientHandler(Socket socket, ServerController serverController) {
        super(serverController);
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());
            handleConnection();

            while (!socket.isClosed()) {
                ClientRequest request = (ClientRequest) in.readObject();
                try {
                    processRequest(request);
                } catch (RuntimeException e) {
                    System.err.println("[SOCKET] Error processing request from "
                            + getClientUsername() + ": " + e);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("[SOCKET] Client disconnected or network error: " + e.getMessage());
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
            out.writeObject(response);
            out.flush();
            out.reset();
        } catch (IOException e) {
            System.err.println("ERROR: [SOCKET] Failed to send response to: " + getClientUsername());
        }
    }
}
