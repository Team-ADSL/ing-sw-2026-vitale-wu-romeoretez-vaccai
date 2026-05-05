package org.adsl;

import org.adsl.client.AppCoordinator;
import org.adsl.client.network.ServerConnection;
import org.adsl.client.network.fake.FakeServerConnection;
import org.adsl.client.network.rmi.RMIServerConnection;
import org.adsl.client.network.socket.SocketClientConnection;
import org.adsl.client.view.GameUI;
import org.adsl.client.view.tui.TUI;
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

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class App 
{
    static void main( String[] args )
    {
        if (args.length == 0) {
            printUsageAndExit("Parameter missing.");
        }

        String mode = args[0].toLowerCase();
        switch (mode) {
            case "--test-tui" -> startTestTui();
            case "--test-server-connection" -> startClientTester();
            case "--server" -> {
                // Expected args: --server <socket-port> <rmi-port> <recover-directory>
                if (args.length != 4) {
                    printUsageAndExit("Invalid server's parameter or missing..");
                }

                int socketPort = 0;
                int rmiPort = 0;
                try {
                    socketPort = Integer.parseInt(args[1]);
                    rmiPort = Integer.parseInt(args[2]);

                    if (socketPort < 1024 || socketPort > 65535 || rmiPort < 1024 || rmiPort > 65535) {
                        printUsageAndExit("Use ports between 1024 and 65535.");
                    }
                    if (socketPort == rmiPort) {
                        printUsageAndExit("Socket and RMI cannot share the same port.");
                    }
                } catch (NumberFormatException e) {
                    printUsageAndExit("The ports specified are not valid integer numbers.");
                }
                String saveDirectory = args[3];
                startServer(socketPort, rmiPort, saveDirectory);
            }
            case "--client" -> {
                // Expected args: --client <connection> <ui> <server-ip> <server-port>
                if (args.length != 5) {
                    printUsageAndExit("Invalid client's parameter or missing.");
                }
                String connectionType = args[1].toLowerCase();
                String uiType = args[2].toLowerCase();
                String ipAddress = args[3];

                if (!"--socket".equals(connectionType) && !"--rmi".equals(connectionType)) {
                    printUsageAndExit("Invalid connection type. Use --socket or --rmi.");
                }

                if (!"--tui".equals(uiType) && !"--gui".equals(uiType)) {
                    printUsageAndExit("Invalid UI type. Use --tui or --gui.");
                }

                int port = 0;
                try {
                    port = Integer.parseInt(args[4]);
                    if (port < 1024 || port > 65535) {
                        printUsageAndExit("Use a port between 1024 and 65535.");
                    }
                } catch (NumberFormatException e) {
                    printUsageAndExit("The ports specified is not valid integer numbers.");
                }

                startClient(connectionType, uiType, ipAddress, port);
            }
            default -> printUsageAndExit("Unknown mode: " + mode);
        }
    }

    public static void startClientTester() {
        String ip = "127.0.0.1";
        int port = 8080;

        System.out.println("Trying connection at " + ip + ":" + port + "...");

        try (Socket socket = new Socket(ip, port);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {

            out.flush();
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            System.out.println("Connection established.");

            Object response = in.readObject();
            System.out.println("Received from server: " + response.getClass().getSimpleName());

        } catch (Exception e) {
            System.err.println("Connection error: " + e.getMessage());
        }
    }

    private static void startTestTui() {
        System.out.println("[BUILDING] Starting MESOS in test mode (TUI + in-process fake server)...");
        TUI tui = new TUI();
        ServerConnection fakeServerConnection = new FakeServerConnection();
        AppCoordinator appCoordinator = new AppCoordinator(tui, fakeServerConnection);
        fakeServerConnection.setAppCoordinator(appCoordinator);
        tui.setAppCoordinator(appCoordinator);

        Runtime.getRuntime().addShutdownHook(new Thread(tui::shutdown));
        tui.start();
    }

    private static void startServer(int socketPort, int rmiPort, String recoverDirectory) {
        System.out.println("Starting server...");
        try {
            DatabaseManager.initDatabase();

            // Setup Socket Server
            ExecutorService threadPool = Executors.newCachedThreadPool();
            SocketServer socketServer = new SocketServer(threadPool, socketPort);
            Thread socketThread = new Thread(socketServer);
            socketThread.start();
            System.out.println("- Socket Server listening on port " + socketPort);

            // Setup RMI
            Registry registry = LocateRegistry.createRegistry(rmiPort);
            RemoteServerServiceImpl rmiServer = new RemoteServerServiceImpl();
            registry.rebind("GameServer", rmiServer);
            System.out.println("- RMI Server listening on port " + rmiPort + " with name 'GameServer'");

            // Setup server controller
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

    private static void startClient(String connectionType, String uiType, String ipAddress, int port) {
        System.out.println("Starting Client...");
        System.out.println("- Network: " + connectionType.replace("--", "").toUpperCase());
        System.out.println("- UI: " + uiType.replace("--", "").toUpperCase());
        System.out.println("- Server IP: " + ipAddress);
        System.out.println("- Server Port: " + port); // Added port print

        try {
            GameUI gameUI;
            if ("--gui".equals(uiType)) {
                // gameUI = new GUI();
                return;
            } else {
                gameUI = new TUI();
            }

            ServerConnection serverConnection;
            if ("--socket".equals(connectionType)) {
                serverConnection = new SocketClientConnection();
            } else {
                serverConnection = new RMIServerConnection();
            }
            AppCoordinator appCoordinator = new AppCoordinator(gameUI, serverConnection);
            serverConnection.setAppCoordinator(appCoordinator);
            gameUI.setAppCoordinator(appCoordinator);
            appCoordinator.setConnectionParams(ipAddress, port);
            appCoordinator.startPingScheduler(5000, 10000);
            serverConnection.connect(ipAddress, port);
            gameUI.start();
            System.out.println("UI Application started.");

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\nClosing signal. Starting Shutdown...");
                gameUI.shutdown();
                try {
                    appCoordinator.disconnect();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }));
        } catch (Exception e) {
            System.err.println("Critical error during client starting: " + e.getMessage());
        }
    }

    // Help messages for proper application use
    private static void printUsageAndExit(String errorMessage) {
        System.err.println("ERROR: " + errorMessage + "\n");
        System.out.println("=== USAGE ===");

        System.out.println("\nTo start as SERVER:");
        System.out.println("java -jar mesos.jar --server <socket-port> <rmi-port> <recover-directory>");
        System.out.println("  <socket-port>       : Network socket listening port (e.g. 8080)");
        System.out.println("  <rmi-port>          : RMI registry listening port (e.g. 1099)");
        System.out.println("  <recover-directory> : Path for recovery files (e.g. ./saves)");

        System.out.println("\nTo start in TEST-TUI mode (same terminal, in-process fake server):");
        System.out.println("java -jar mesos.jar --test-tui");

        System.out.println("\nTo start as CLIENT:");
        System.out.println("java -jar mesos.jar --client <connection> <interface> <ip-server> <port>");
        System.out.println("  <connection> : --socket or --rmi");
        System.out.println("  <interface>  : --tui or --gui");
        System.out.println("  <ip-server>  : IP address of the server (e.g. 127.0.0.1)");
        System.out.println("  <port>       : Port of the server (e.g. 8080 or 1099)");

        System.exit(1);
    }
}