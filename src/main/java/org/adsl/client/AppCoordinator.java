package org.adsl.client;

import org.adsl.client.network.ServerConnection;
import org.adsl.client.view.GameUI;
import org.adsl.client.exceptions.InvalidResponseException;
import org.adsl.client.view.tui.prova.Event;
import org.adsl.shared.network.requests.*;
import org.adsl.shared.network.responses.*;
import org.adsl.shared.utils.Move;

import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

// Mediator between Network and View. Send response to UI and request to Network.
public class AppCoordinator implements ResponseVisitor{
    private final GameUI gameUI;
    private final ServerConnection serverConnection;
    private ScheduledExecutorService pingScheduler;
    private volatile long lastServerPing = System.currentTimeMillis();

    public AppCoordinator(GameUI gameUI, ServerConnection serverConnection) {
        this.gameUI = gameUI;
        this.serverConnection = serverConnection;
        this.pingScheduler = null;
    }

    public void startPingScheduler(int pingRatioMs, long serverTimeoutMs) {
        pingScheduler = Executors.newSingleThreadScheduledExecutor();

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
        gameUI.onHomeUpdate(response.getActiveGames());
    }
    @Override
    public void visit(LobbyUpdate response) throws InvalidResponseException {
        gameUI.onLobbyUpdate(response.getPlayers());
    }
    @Override
    public void visit(GameUpdate response) throws InvalidResponseException {
        //gameUI.onGameUpdate(response.getGame());
        Event event = new GameUpdateEvent();
        gameUI.handleEvent(event);
    }
    @Override
    public void visit(GameEnded response) throws InvalidResponseException {
        gameUI.onEndGame(response.getResults());
    }
    @Override
    public void visit(ErrorResponse response) throws InvalidResponseException {
        gameUI.onErrorReceived(response.getMessage());
    }
    @Override
    public void visit(ServerDisconnected response) throws InvalidResponseException {
        gameUI.handleDisconnection();
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
    public void makeMoveRequest(Set<Move> moves) throws Exception {
        ClientRequest clientRequest = new MoveRequest(moves);
        serverConnection.sendRequest(clientRequest);
    }
    public void disconnect() throws Exception {
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
}
