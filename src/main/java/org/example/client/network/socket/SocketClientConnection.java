package org.example.client.network.socket;

import org.example.client.AppCoordinator;
import org.example.client.network.ServerConnection;
import org.example.shared.network.requests.ClientRequest;
import org.example.shared.network.responses.ServerDisconnected;
import org.example.shared.network.responses.ServerResponse;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class SocketClientConnection implements ServerConnection {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private AppCoordinator appCoordinator;
    private boolean isRunning;

    @Override
    public void connect(String ip, int port) throws Exception {
        socket = new Socket(ip, port);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
        isRunning = true;
        new Thread(this::listenToServer).start();
    }

    private void listenToServer() {
        try {
            while (isRunning && !socket.isClosed()) {
                ServerResponse response = (ServerResponse) in.readObject();
                if (appCoordinator != null) {
                    response.accept(appCoordinator);
                }
            }
        } catch (Exception e) {
            if (isRunning && appCoordinator != null) {
                ServerResponse disconnection = new ServerDisconnected();
                disconnection.accept(appCoordinator);
            }
        }
    }

    @Override
    public void sendRequest(ClientRequest request) throws Exception {
        out.writeObject(request);
        out.flush();
    }

    @Override
    public void disconnect() throws Exception {
        isRunning = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    @Override
    public void setAppCoordinator(AppCoordinator appCoordinator) {
        this.appCoordinator = appCoordinator;
    }
}
