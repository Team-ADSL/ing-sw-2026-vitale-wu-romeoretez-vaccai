package org.example.client.network.socket;

import org.example.client.network.ServerConnection;
import org.example.client.view.ClientListener;
import org.example.shared.model.DatasourceDTO;
import org.example.shared.network.requests.ClientRequest;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class SocketClientConnection implements ServerConnection {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private final ClientListener listener;
    private boolean isRunning;

    public SocketClientConnection(ClientListener listener) {
        this.listener = listener;
    }

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
                DatasourceDTO incomingData = (DatasourceDTO) in.readObject();
                if (listener != null) {
                    listener.onDataReceived(incomingData);
                }
            }
        } catch (Exception e) {
            if (isRunning && listener != null) {
                listener.onDisconnected();
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
}
