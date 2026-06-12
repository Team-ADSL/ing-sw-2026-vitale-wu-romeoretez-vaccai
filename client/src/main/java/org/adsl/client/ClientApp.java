package org.adsl.client;

import org.adsl.client.network.ServerConnection;
import org.adsl.client.network.fake.FakeServerConnection;
import org.adsl.client.network.rmi.RMIServerConnection;
import org.adsl.client.network.socket.SocketClientConnection;
import org.adsl.client.view.GameUI;
import org.adsl.client.view.gui.GUI;
import org.adsl.client.view.tui.TUI;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Client entry point. Replaces the {@code --client}, {@code --test-tui},
 * {@code --test-gui} and {@code --test-server-connection} branches of the old
 * shared {@code App} class so the client jar no longer drags in server code.
 *
 * Usage:
 *   java -jar mesos-client.jar --client <socket|rmi> <tui|gui> <ip> <port>
 *   java -jar mesos-client.jar --test-tui
 *   java -jar mesos-client.jar --test-gui
 */
public final class ClientApp {

    private ClientApp() {}

    static void main(String[] args) {
        if (args.length == 0) {
            printUsageAndExit("Mode missing.");
        }

        String mode = args[0].toLowerCase();
        switch (mode) {
            case "--test-tui" -> startTestTui();
            case "--test-gui" -> startTestGui();
            case "--client" -> {
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

                int port;
                try {
                    port = Integer.parseInt(args[4]);
                } catch (NumberFormatException e) {
                    printUsageAndExit("The port specified is not a valid integer.");
                    return;
                }
                if (port < 1024 || port > 65535) {
                    printUsageAndExit("Use a port between 1024 and 65535.");
                }

                startClient(connectionType, uiType, ipAddress, port);
            }
            default -> printUsageAndExit("Unknown mode: " + mode);
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

    private static void startTestGui() {
        System.out.println("[BUILDING] Starting MESOS in test mode (GUI + in-process fake server)...");
        GUI gui = new GUI();
        ServerConnection fakeServerConnection = new FakeServerConnection();
        AppCoordinator appCoordinator = new AppCoordinator(gui, fakeServerConnection);
        fakeServerConnection.setAppCoordinator(appCoordinator);
        gui.setAppCoordinator(appCoordinator);

        Runtime.getRuntime().addShutdownHook(new Thread(gui::shutdown));
        gui.start();
    }

    private static void startClient(String connectionType, String uiType, String ipAddress, int port) {
        System.out.println("Starting Client...");
        System.out.println("- Network: " + connectionType.replace("--", "").toUpperCase());
        System.out.println("- UI: " + uiType.replace("--", "").toUpperCase());
        System.out.println("- Server IP: " + ipAddress);
        System.out.println("- Server Port: " + port);

        try {
            GameUI gameUI = "--gui".equals(uiType) ? new GUI() : new TUI();
            ServerConnection serverConnection = "--socket".equals(connectionType)
                    ? new SocketClientConnection()
                    : new RMIServerConnection();

            AppCoordinator appCoordinator = new AppCoordinator(gameUI, serverConnection);
            serverConnection.setAppCoordinator(appCoordinator);
            gameUI.setAppCoordinator(appCoordinator);
            appCoordinator.setConnectionParams(ipAddress, port);
            serverConnection.connect(ipAddress, port);
            appCoordinator.startPingScheduler(5000, 10000);
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

    private static void printUsageAndExit(String errorMessage) {
        System.err.println("ERROR: " + errorMessage + "\n");
        System.out.println("=== USAGE ===");
        System.out.println("\nReal client:");
        System.out.println("java -jar mesos-client.jar --client <socket|rmi> <tui|gui> <ip> <port>");
        System.out.println("  <socket|rmi> : --socket or --rmi");
        System.out.println("  <tui|gui>    : --tui or --gui");
        System.out.println("  <ip>         : server address (e.g. 127.0.0.1)");
        System.out.println("  <port>       : server port (e.g. 8080 or 1099)");
        System.out.println("\nIn-process test modes:");
        System.out.println("java -jar mesos-client.jar --test-tui");
        System.out.println("java -jar mesos-client.jar --test-gui");
        System.exit(1);
    }
}
