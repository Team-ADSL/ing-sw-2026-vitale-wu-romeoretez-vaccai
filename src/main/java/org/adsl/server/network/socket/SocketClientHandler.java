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
                processRequest(request);
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Client disconnected or network error: " + e.getMessage());
            handleDisconnection();
        }
    }

    @Override
    public void closeConnection() {
        try {
            socket.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void sendResponse(ServerResponse response) {
        try {
            out.writeObject(response);
            out.flush();
            out.reset();
        } catch (IOException e) {
            System.err.println("Impossible to send the message.");
        }
    }
}
