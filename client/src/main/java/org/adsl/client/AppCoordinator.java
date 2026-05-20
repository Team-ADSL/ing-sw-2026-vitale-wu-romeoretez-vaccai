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

    public void setConnectionParams(String ip, int port) {
        this.lastIp = ip;
        this.lastPort = port;
    }

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
                System.err.println("Error during ping sending: " + e.getMessage());
            }
        }, pingRatioMs, pingRatioMs, TimeUnit.MILLISECONDS);
    }

    public void stopPingScheduler() {
        if (pingScheduler != null) {
            pingScheduler.shutdownNow();
        }
    }

    public void handleServerResponse(ServerResponse serverResponse){
        lastServerPing = System.currentTimeMillis();
        // Heartbeats (ping) are kept out of the pacer: they are not user-visible
        // and any delay would break the timeout watchdog above.
        if (serverResponse.isHeartbeat()) {
            dispatchResponse(serverResponse);
            return;
        }

        long delayMs;
        synchronized (pacerLock) {
            long now = System.currentTimeMillis();
            long scheduled = Math.max(now, nextDispatchAtMs);
            delayMs = scheduled - now;
            nextDispatchAtMs = scheduled + DISPATCH_MIN_DELAY_MS;
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

    public void reconnect() throws Exception {
        stopPingScheduler();
        try { serverConnection.disconnect(); } catch (Exception ignored) {}
        lastServerPing = System.currentTimeMillis();
        serverConnection.connect(lastIp, lastPort);
        startPingScheduler(lastPingRatioMs, lastServerTimeoutMs);
    }

    // Creating and forwarding ClientRequest to the Server
    public void connectRequest() throws Exception {
        ClientRequest clientRequest = new ClientConnection();
        serverConnection.sendRequest(clientRequest);
    }
    public void createLoginRequest(String username) throws Exception {
        ClientRequest clientRequest = new LoginRequest(username);
        serverConnection.sendRequest(clientRequest);
    }
    public void createLogoutRequest() throws Exception {
        ClientRequest clientRequest = new LogoutRequest();
        serverConnection.sendRequest(clientRequest);
    }
    public void createExitLobbyRequest() throws Exception {
        ClientRequest clientRequest = new ExitLobbyRequest();
        serverConnection.sendRequest(clientRequest);
    }
    public void createGameRequest(int numPlayer) throws Exception {
        ClientRequest clientRequest = new CreateGameRequest(numPlayer);
        serverConnection.sendRequest(clientRequest);
    }
    public void enterGameRequest(int gameId) throws Exception {
        ClientRequest clientRequest = new EnterGameRequest(gameId);
        serverConnection.sendRequest(clientRequest);
    }
    public void startGameRequest() throws Exception {
        ClientRequest clientRequest = new StartGameRequest();
        serverConnection.sendRequest(clientRequest);
    }
    public void createTotemPickingRequest(Totem totem) throws Exception {
        ClientRequest clientRequest = new TotemPickingRequest(totem);
        serverConnection.sendRequest(clientRequest);
    }
    public void makeMoveRequest(Set<Move> moves) throws Exception {
        ClientRequest clientRequest = new MoveRequest(moves);
        serverConnection.sendRequest(clientRequest);
    }
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
    public void createExitGameRequest() throws Exception {
        ClientRequest clientRequest = new ExitGameRequest();
        serverConnection.sendRequest(clientRequest);
    }
}
