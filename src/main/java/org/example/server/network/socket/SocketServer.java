package org.example.server.network.socket;

import org.example.server.controller.ServerController;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;

public class SocketServer implements Runnable {
    private final ServerController serverController;
    private final ExecutorService threadPool;
    private boolean active;
    private final int port;
    private ServerSocket serverSocket;

    public SocketServer(ServerController serverController, ExecutorService threadPool, int port) {
        this.serverController = serverController;
        this.threadPool = threadPool;
        this.active = true;
        this.port = port;
    }

    @Override
    public void run() {
        try{
            this.serverSocket = new ServerSocket(port);
            System.out.println("SocketServer active on port: " + port);

            while (active) {
                Socket client = serverSocket.accept();
                System.out.println("New connection: " + client.getInetAddress());
                SocketClientHandler clientHandler = new SocketClientHandler(client, serverController);
                threadPool.submit(clientHandler);
            }
        } catch (SocketException e) {
            System.out.println("SocketServer stopped: closing...");
        } catch (IOException e){
            System.out.println("SocketServer error: " + e.getMessage() + ". Shutdown.");
        } finally {
            threadPool.shutdown();
        }

    }

    public void stopServer() {
        this.active = false;
        try {
            if (this.serverSocket != null && !this.serverSocket.isClosed()) {
                this.serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error during ServerSocket close: " + e.getMessage());
        }

        if (this.threadPool != null && !this.threadPool.isShutdown()) {
            this.threadPool.shutdown();
        }
        System.out.println("Socket Server successfully stopped.");
    }
}
