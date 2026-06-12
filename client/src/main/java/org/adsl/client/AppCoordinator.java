package org.adsl.client;

import org.adsl.client.network.ServerConnection;
import org.adsl.client.view.GameUI;
import org.adsl.shared.exceptions.InvalidResponseException;
import org.adsl.shared.enums.Totem;
import org.adsl.shared.network.requests.*;
import org.adsl.shared.network.responses.*;
import org.adsl.shared.utils.Move;

import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Mediator between the network layer and the view layer.
 * <p>
 * Implements {@code ResponseVisitor} to receive {@code ServerResponse} objects
 * from the network thread, convert them to view-layer events, and forward them
 * to the active {@code GameUI}. Also serialises {@code ClientRequest} objects
 * sent by the UI back to the server via the {@code ServerConnection}.
 * </p>
 * <p>
 * A built-in dispatch pacer spaces UI-bound responses at least
 * {@code DISPATCH_MIN_DELAY_MS} apart so event overlays are readable.
 * Heartbeat (ping) responses bypass the pacer. A background ping scheduler
 * sends keep-alive requests at a fixed rate and triggers disconnection when
 * the server stops responding.
 * </p>
 */
public class AppCoordinator implements ResponseVisitor{

    /**
     * Minimum interval between two consecutive UI-bound dispatches. When the
     * server fires many responses back-to-back (e.g. multiple events resolved
     * in a single end-of-round) the pacer spaces them out so the user can
     * actually read each one before the next replaces it.
     */
    private static final long DISPATCH_MIN_DELAY_MS = 1200L;

    /**
     * Longer hold for the orange event-resolution overlay
     * ({@link ServerResponse#isEventOverlay()}): it stays on screen until the
     * next response replaces it, so this is how long players get to read the
     * per-player food/PP deltas. Wider than {@link #DISPATCH_MIN_DELAY_MS}
     * because up to five players' changes may need reading at once.
     */
    private static final long EVENT_DISPATCH_DELAY_MS = 3000L;

    private final GameUI gameUI;
    private final ServerConnection serverConnection;
    private ScheduledExecutorService pingScheduler;
    private ScheduledExecutorService dispatchPacer;
    private volatile long lastServerPing = System.currentTimeMillis();
    private String lastIp;
    private int lastPort;
    private int lastPingRatioMs = 5000;
    private long lastServerTimeoutMs = 10000;

    private final Object pacerLock = new Object();
    /** Wall-clock time (ms) of the next slot the pacer has reserved for a dispatch. */
    private long nextDispatchAtMs = 0L;

    private final AtomicBoolean disconnected = new AtomicBoolean(false);

    /**
     * @param gameUI           the active UI to forward server updates to
     * @param serverConnection the transport used to send requests to the server
     */
    public AppCoordinator(GameUI gameUI, ServerConnection serverConnection) {
        this.gameUI = gameUI;
        this.serverConnection = serverConnection;
        this.pingScheduler = null;
        this.dispatchPacer = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "response-pacer");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Stores the server address used by {@link #reconnect()}.
     *
     * @param ip   server hostname or IP address
     * @param port server port
     */
    public void setConnectionParams(String ip, int port) {
        this.lastIp = ip;
        this.lastPort = port;
    }

    /**
     * Starts a background task that periodically pings the server and
     * triggers {@link GameUI#onServerDisconnected()} if no response is seen
     * within {@code serverTimeoutMs}.
     *
     * @param pingRatioMs     interval between pings, in milliseconds
     * @param serverTimeoutMs maximum time since the last server activity before
     *                        the connection is considered lost, in milliseconds
     */
    public void startPingScheduler(int pingRatioMs, long serverTimeoutMs) {
        this.lastPingRatioMs = pingRatioMs;
        this.lastServerTimeoutMs = serverTimeoutMs;
        pingScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ping-scheduler");
            t.setDaemon(true);
            return t;
        });

        pingScheduler.scheduleAtFixedRate(() -> {
            try {
                long now = System.currentTimeMillis();

                if (now - lastServerPing > serverTimeoutMs) {
                    System.err.println("Server unavailable (Timeout). Disconnection...");
                    gameUI.onServerDisconnected();
                    return;
                }

                serverConnection.sendRequest(new ClientPing());

            } catch (Exception e) {
                // RMI throws here as soon as the server is unreachable
                // (ConnectException / RemoteException). Treat the first
                // failure as disconnection so the UI reacts immediately
                // instead of waiting for serverTimeoutMs to elapse.
                // Socket never reaches this branch — its PrintWriter
                // swallows IOException; the listener thread is what
                // detects socket-side drops.
                System.err.println("Error during ping sending: " + e.getMessage());
                gameUI.onServerDisconnected();
            }
        }, pingRatioMs, pingRatioMs, TimeUnit.MILLISECONDS);
    }

    /** Stops the background ping scheduler, if running. */
    public void stopPingScheduler() {
        if (pingScheduler != null) {
            pingScheduler.shutdownNow();
        }
    }

    /**
     * Single entry point screens can use to terminate the application.
     * Delegates to the active UI's {@code shutdown()} so the GUI X-button
     * crash-avoidance path (consume + cleanup thread + halt) is reused —
     * direct {@code Platform.exit()} from a screen reopens the same
     * AppKit/Glass race on macOS.
     */
    public void requestShutdown() {
        gameUI.shutdown();
    }

    /**
     * Entry point for responses received from the network layer. Heartbeats
     * are forwarded immediately; other responses are paced (see
     * {@link #DISPATCH_MIN_DELAY_MS}, {@link #EVENT_DISPATCH_DELAY_MS}) so the
     * UI has time to display each one.
     *
     * @param serverResponse the response received from the server
     */
    public void handleServerResponse(ServerResponse serverResponse){
        lastServerPing = System.currentTimeMillis();
        // Heartbeats (ping) are kept out of the pacer: they are not user-visible
        // and any delay would break the timeout watchdog above.
        if (serverResponse.isHeartbeat()) {
            dispatchResponse(serverResponse);
            return;
        }

        // The gap reserved after this dispatch is how long this response stays on
        // screen before the next one. Event overlays hold longer than the rest.
        long holdMs = serverResponse.isEventOverlay() ? EVENT_DISPATCH_DELAY_MS : DISPATCH_MIN_DELAY_MS;
        long delayMs;
        synchronized (pacerLock) {
            long now = System.currentTimeMillis();
            long scheduled = Math.max(now, nextDispatchAtMs);
            delayMs = scheduled - now;
            nextDispatchAtMs = scheduled + holdMs;
        }

        if (delayMs <= 0) {
            dispatchResponse(serverResponse);
        } else {
            dispatchPacer.schedule(() -> dispatchResponse(serverResponse),
                    delayMs, TimeUnit.MILLISECONDS);
        }
    }

    private void dispatchResponse(ServerResponse serverResponse) {
        try {
            serverResponse.accept(this);
        } catch(Exception e){
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void visit(ServerPing response) throws InvalidResponseException {}

    // Handle ServerResponse
    @Override
    public void visit(LoginNeeded response) throws InvalidResponseException {
        gameUI.showUsernameField();
    }
    @Override
    public void visit(HomeUpdate response) throws InvalidResponseException {
        gameUI.onHomeUpdate(response.getActiveGames(), response.getGamePlayers(), response.getGameCapacity(), response.getMessage());
    }
    @Override
    public void visit(LobbyUpdate response) throws InvalidResponseException {
        gameUI.onLobbyUpdate(response.getGameId(), response.getPlayers(),
                response.getNumPlayerAllowed(), response.getMessage());
    }
    @Override
    public void visit(TotemAvailableUpdate response) throws InvalidResponseException {
        gameUI.onTotemAvailableUpdate(response.getTotemAvailable(), response.getMessage());
    }
    @Override
    public void visit(GameUpdate response) throws InvalidResponseException {
        gameUI.onGameUpdate(response.getGame(), response.getMessage());
    }
    @Override
    public void visit(GameEnded response) throws InvalidResponseException {
        gameUI.onEndGame(response.getResults(), response.getRecords(), response.getMessage());
    }
    @Override
    public void visit(ErrorResponse response) throws InvalidResponseException {
        gameUI.onErrorReceived(response.getMessage());
    }
    @Override
    public void visit(ServerDisconnected response) throws InvalidResponseException {
        gameUI.onServerDisconnected();
    }

    @Override
    public void visit(EventsTriggered response) throws InvalidResponseException {
        gameUI.onEventTriggered(response.getEventTitle(), response.getLogMessage());
    }

    @Override
    public void visit(GameLogRestore response) throws InvalidResponseException {
        gameUI.onGameLogRestore(response.getHistory());
    }

    /**
     * Re-establishes the connection to the last known server address and
     * restarts the ping scheduler.
     *
     * @throws Exception if the connection attempt fails
     */
    public void reconnect() throws Exception {
        stopPingScheduler();
        try { serverConnection.disconnect(); } catch (Exception ignored) {}
        lastServerPing = System.currentTimeMillis();
        serverConnection.connect(lastIp, lastPort);
        startPingScheduler(lastPingRatioMs, lastServerTimeoutMs);
    }

    // Creating and forwarding ClientRequest to the Server

    /**
     * Sends the initial connection request to the server.
     *
     * @throws Exception if sending the request fails
     */
    public void connectRequest() throws Exception {
        ClientRequest clientRequest = new ClientConnection();
        serverConnection.sendRequest(clientRequest);
    }

    /**
     * Sends a login request for the given username.
     *
     * @param username the username to log in with
     * @throws Exception if sending the request fails
     */
    public void createLoginRequest(String username) throws Exception {
        ClientRequest clientRequest = new LoginRequest(username);
        serverConnection.sendRequest(clientRequest);
    }

    /**
     * Sends a logout request, returning the player to the login state.
     *
     * @throws Exception if sending the request fails
     */
    public void createLogoutRequest() throws Exception {
        ClientRequest clientRequest = new LogoutRequest();
        serverConnection.sendRequest(clientRequest);
    }

    /**
     * Sends a request to leave the current lobby.
     *
     * @throws Exception if sending the request fails
     */
    public void createExitLobbyRequest() throws Exception {
        ClientRequest clientRequest = new ExitLobbyRequest();
        serverConnection.sendRequest(clientRequest);
    }

    /**
     * Sends a request to create a new game.
     *
     * @param numPlayer number of players the new game should support
     * @throws Exception if sending the request fails
     */
    public void createGameRequest(int numPlayer) throws Exception {
        ClientRequest clientRequest = new CreateGameRequest(numPlayer);
        serverConnection.sendRequest(clientRequest);
    }

    /**
     * Sends a request to join an existing game.
     *
     * @param gameId ID of the game to join
     * @throws Exception if sending the request fails
     */
    public void enterGameRequest(int gameId) throws Exception {
        ClientRequest clientRequest = new EnterGameRequest(gameId);
        serverConnection.sendRequest(clientRequest);
    }

    /**
     * Sends a request to start the current game.
     *
     * @throws Exception if sending the request fails
     */
    public void startGameRequest() throws Exception {
        ClientRequest clientRequest = new StartGameRequest();
        serverConnection.sendRequest(clientRequest);
    }

    /**
     * Sends a request to pick a totem during the totem-picking phase.
     *
     * @param totem the chosen totem
     * @throws Exception if sending the request fails
     */
    public void createTotemPickingRequest(Totem totem) throws Exception {
        ClientRequest clientRequest = new TotemPickingRequest(totem);
        serverConnection.sendRequest(clientRequest);
    }

    /**
     * Sends the set of moves the player wants to perform this turn.
     *
     * @param moves the moves to submit
     * @throws Exception if sending the request fails
     */
    public void makeMoveRequest(Set<Move> moves) throws Exception {
        ClientRequest clientRequest = new MoveRequest(moves);
        serverConnection.sendRequest(clientRequest);
    }

    /**
     * Notifies the server of a clean disconnection and tears down the local
     * connection. Idempotent: subsequent calls are no-ops.
     *
     * @throws Exception if sending the disconnect notification fails
     */
    public void disconnect() throws Exception {
        // Idempotent: X-button path (GUI.shutdown cleanup thread) and JVM
        // shutdown hook (ClientApp) can both reach this; second call is a no-op
        // instead of double-disconnecting on already-torn-down transport.
        if (!disconnected.compareAndSet(false, true)) return;
        stopPingScheduler();
        if (dispatchPacer != null) {
            dispatchPacer.shutdownNow();
        }
        try {
            ClientRequest clientRequest = new ClientDisconnected();
            serverConnection.sendRequest(clientRequest);
        } catch (Exception e) {
            System.err.println("Error sending disconnection message to server");
        } finally {
            try {
                serverConnection.disconnect();
            } catch (Exception e) {
                System.err.println("Error during local connection closing.");
            }
        }
    }
    /**
     * Sends a request to exit the current game.
     *
     * @throws Exception if sending the request fails
     */
    public void createExitGameRequest() throws Exception {
        ClientRequest clientRequest = new ExitGameRequest();
        serverConnection.sendRequest(clientRequest);
    }
}
