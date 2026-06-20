package org.adsl.server.network.socket;

import org.adsl.server.controller.ServerController;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;

/**
 * TCP server that listens for incoming client connections on the configured
 * port and creates a {@link SocketClientHandler} for each accepted socket.
 * <p>
 * Runs on a dedicated thread; each handler is submitted to the shared
 * {@link java.util.concurrent.ExecutorService}. Shutdown closes the
 * {@link ServerSocket}, causing the blocking {@code accept()} call to throw
 * a {@link java.net.SocketException} and exit the loop cleanly.
 * </p>
 */
public class SocketServer implements Runnable {
    private ServerController serverController;
    private final ExecutorService threadPool;
    private boolean active;
    private final int port;
    private ServerSocket serverSocket;

    /**
     * Creates a socket server bound to the given port. The server controller
     * must be set afterwards via {@link #setServerController}.
     *
     * @param threadPool shared pool used to run client handler threads
     * @param port       TCP port to listen on
     */
    public SocketServer(ExecutorService threadPool, int port) {
        this.serverController = null;
        this.threadPool = threadPool;
        this.active = true;
        this.port = port;
    }

    @Override
    public void run() {
        try{
            this.serverSocket = new ServerSocket(port);

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

    /**
     * Stops the accept loop and releases resources: closes the
     * {@link ServerSocket} (causing {@link #run} to exit) and shuts down the
     * shared thread pool.
     */
    public void shutdown() {
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

    /**
     * Sets the controller passed to each new {@link SocketClientHandler}.
     *
     * @param serverController the server controller instance
     */
    public void setServerController(ServerController serverController) {
        this.serverController = serverController;
    }
}
