package org.example.server.network.socket;

import org.example.server.controller.ServerController;
import org.example.server.network.VirtualClient;
import org.example.shared.network.requests.ClientRequest;
import org.example.shared.network.responses.ServerResponse;

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

            while (!socket.isClosed()) {
                ClientRequest request = (ClientRequest) in.readObject();
                processRequest(request);
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Client disconnesso o errore di rete: " + e.getMessage());
            handleDisconnection();
        } finally {
            closeConnection();
        }
    }

    private void closeConnection() {
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
        } catch (IOException e) {
            System.err.println("Impossible to send the message.");
        }
    }
}
