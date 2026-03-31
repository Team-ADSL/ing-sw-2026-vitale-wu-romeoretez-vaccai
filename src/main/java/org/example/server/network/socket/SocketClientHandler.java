package org.example.server.network.socket;

import org.example.server.controller.ServerController;
import org.example.server.model.Datasource;
import org.example.server.network.VirtualClient;
import org.example.shared.model.MatchResult;
import org.example.shared.network.requests.ClientRequest;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;

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
            e.printStackTrace();
        }
    }

    @Override
    public void update(Datasource datasource) {
        try {
            out.writeObject(datasource.createDTO());
            out.flush();
        } catch (IOException e) {
            System.err.println("Impossible to send the message.");
        }
    }

    @Override
    public void sendErrorMessage(String error) {
        try {
            out.writeObject(error);
            out.flush();
        } catch (IOException e) {
            System.err.println("Impossible to send the message.");
        }
    }

    @Override
    public void update(int gameId, List<MatchResult> matchResults) {
        try {
            out.writeObject(matchResults);
            out.flush();
        } catch (IOException e) {
            System.err.println("Impossible to send the message.");
        }
    }
}
