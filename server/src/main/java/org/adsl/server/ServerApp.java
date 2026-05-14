package org.adsl.server;

import org.adsl.server.config.BoardConfigLoader;
import org.adsl.server.config.JsonBoardConfigLoader;
import org.adsl.server.controller.ServerController;
import org.adsl.server.db.ConnectionProvider;
import org.adsl.server.db.DatabaseConfig;
import org.adsl.server.db.DatabaseManager;
import org.adsl.server.model.Home;
import org.adsl.server.network.rmi.RemoteServerServiceImpl;
import org.adsl.server.network.socket.SocketServer;
import org.adsl.server.persistence.GameDAO;
import org.adsl.server.persistence.GamePersistenceManager;
import org.adsl.server.persistence.SerialGamePersistenceManager;
import org.adsl.server.persistence.SqlGameDAO;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Server entry point. Replaces the {@code --server} branch of the old shared
 * {@code App} class so the server jar no longer drags in client code.
 *
 * Usage:
 *   java -jar mesos-server.jar <socket-port> <rmi-port> <recover-directory>
 */
public final class ServerApp {

    private ServerApp() {}

    public static void main(String[] args) {
        if (args.length != 3) {
            printUsageAndExit("Expected 3 arguments.");
        }

        int socketPort;
        int rmiPort;
        try {
            socketPort = Integer.parseInt(args[0]);
            rmiPort = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            printUsageAndExit("The ports specified are not valid integer numbers.");
            return;
        }

        if (socketPort < 1024 || socketPort > 65535 || rmiPort < 1024 || rmiPort > 65535) {
            printUsageAndExit("Use ports between 1024 and 65535.");
        }
        if (socketPort == rmiPort) {
            printUsageAndExit("Socket and RMI cannot share the same port.");
        }

        String saveDirectory = args[2];
        startServer(socketPort, rmiPort, saveDirectory);
    }

    private static void startServer(int socketPort, int rmiPort, String recoverDirectory) {
        System.out.println("Starting server...");
        try {
            DatabaseManager.initDatabase();

            ExecutorService threadPool = Executors.newCachedThreadPool();
            SocketServer socketServer = new SocketServer(threadPool, socketPort);
            Thread socketThread = new Thread(socketServer);
            socketThread.start();
            System.out.println("- Socket Server listening on port " + socketPort);

            Registry registry = LocateRegistry.createRegistry(rmiPort);
            RemoteServerServiceImpl rmiServer = new RemoteServerServiceImpl();
            registry.rebind("GameServer", rmiServer);
            System.out.println("- RMI Server listening on port " + rmiPort + " with name 'GameServer'");

            ConnectionProvider connectionProvider = DatabaseConfig::getConnection;
            GameDAO gameDAO = new SqlGameDAO(connectionProvider);
            BoardConfigLoader boardConfigLoader = new JsonBoardConfigLoader();
            GamePersistenceManager gamePersistenceManager = new SerialGamePersistenceManager(recoverDirectory);
            Home home = new Home();
            ServerController serverController = new ServerController(
                    home, gameDAO, boardConfigLoader, gamePersistenceManager, registry, rmiServer, socketServer);
            serverController.recoverGames();
            serverController.startTimeoutChecker(5000, 20000);

            socketServer.setServerController(serverController);
            rmiServer.setServerController(serverController);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\nClosing signal. Starting Shutdown...");
                serverController.shutdown();
            }));

            System.out.println("Server started successfully. Waiting connections...");
        } catch (Exception e) {
            System.err.println("Critical error during server starting: " + e.getMessage());
        }
    }

    private static void printUsageAndExit(String errorMessage) {
        System.err.println("ERROR: " + errorMessage + "\n");
        System.out.println("=== USAGE ===");
        System.out.println("java -jar mesos-server.jar <socket-port> <rmi-port> <recover-directory>");
        System.out.println("  <socket-port>       : Network socket listening port (e.g. 8080)");
        System.out.println("  <rmi-port>          : RMI registry listening port (e.g. 1099)");
        System.out.println("  <recover-directory> : Path for recovery files (e.g. ./saves)");
        System.exit(1);
    }
}
