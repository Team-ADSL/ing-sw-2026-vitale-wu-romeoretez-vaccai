package org.adsl.server.network;

import org.adsl.server.controller.ServerController;
import org.adsl.server.model.EndGameObserver;
import org.adsl.server.model.Game;
import org.adsl.server.model.GameObserver;
import org.adsl.shared.enums.Totem;
import org.adsl.shared.model.DBRecord;
import org.adsl.shared.model.MatchResult;
import org.adsl.shared.network.requests.ClientConnection;
import org.adsl.shared.network.requests.ClientDisconnected;
import org.adsl.shared.network.requests.ClientRequest;
import org.adsl.shared.network.responses.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Transport-agnostic representation of a connected client on the server side.
 * <p>
 * Implements {@link GameObserver}, {@link HomeObserver}, and
 * {@link EndGameObserver} to receive push notifications from the model and
 * convert them into {@link ServerResponse} objects that are forwarded to the
 * actual network layer via {@link #sendResponse(ServerResponse)}.
 * </p>
 * <p>
 * Concrete subclasses ({@code SocketClientHandler}, {@code RMIClientHandler})
 * implement {@link #sendResponse} and {@link #closeConnection} for their
 * respective transport.
 * </p>
 * <p>
 * The {@code lastPing} timestamp is volatile and updated on every ping to allow
 * the timeout checker in {@link ServerController} to detect stale connections.
 * </p>
 */
public abstract class VirtualClient implements GameObserver, HomeObserver, EndGameObserver {
    private final ServerController serverController;
    private String clientUsername;
    private Integer gameId;
    private boolean isConnected;
    private volatile long lastPing;

    /**
     * Creates a new virtual client bound to the given controller, marking it as
     * connected and recording the current time as the last ping.
     *
     * @param serverController controller used to dispatch requests from this client
     */
    public VirtualClient(ServerController serverController){
        this.serverController = serverController;
        this.clientUsername = null;
        this.isConnected = true;
        this.gameId = null;
        this.lastPing = System.currentTimeMillis();
    }

    /**
     * Forwards a request from this client to the {@link ServerController}, unless
     * the client has already disconnected.
     *
     * @param req the request received from this client
     */
    public void processRequest(ClientRequest req){
        if(isConnected){
            serverController.handleClientRequest(req, this);
        }
    }

    /**
     * Marks this client as disconnected (if not already) and notifies the
     * {@link ServerController} via a {@link ClientDisconnected} request so it can
     * clean up game/lobby state for this client.
     */
    public void handleDisconnection() {
        if(isConnected){
            setConnected(false);

            System.out.println("[NETWORK] Disconnection initiated for: " + getClientUsername());
            ClientRequest disconnection = new ClientDisconnected();

            serverController.handleClientRequest(disconnection, this);
        }
    }

    /**
     * Called when a new transport connection is established. Sends a
     * {@link ClientConnection} request to the controller so it can start
     * tracking this client (e.g. send the home screen state).
     */
    public void handleConnection() {
        System.out.println("[NETWORK] New connection established.");
        ClientConnection connection = new ClientConnection();
        processRequest(connection);
    }

    // Creating and forwarding ServerResponse to the Client

    /** Sends a {@link LoginNeeded} response, prompting this client to log in. */
    public void sendLoginNeededResponse(){
        ServerResponse serverResponse = new LoginNeeded();
        this.sendResponse(serverResponse);
    }

    @Override
    public void updateHome(List<Integer> activeGames){
        updateHome(activeGames, null);
    }

    @Override
    public void updateHome(List<Integer> activeGames, String message){
        HomeUpdate serverResponse = new HomeUpdate(activeGames);
        if (message != null) serverResponse.setMessage(message);
        System.out.println("[SENDING] Home update:" + serverResponse + " to: " + getClientUsername());
        this.sendResponse(serverResponse);
    }

    @Override
    public void updateHome(List<Integer> activeGames, Map<Integer, List<String>> gamePlayers, Map<Integer, Integer> gameCapacity) {
        updateHome(activeGames, gamePlayers, gameCapacity, null);
    }

    @Override
    public void updateHome(List<Integer> activeGames, Map<Integer, List<String>> gamePlayers, Map<Integer, Integer> gameCapacity, String message) {
        HomeUpdate serverResponse = new HomeUpdate(activeGames, gamePlayers, gameCapacity);
        if (message != null) serverResponse.setMessage(message);
        System.out.println("[SENDING] Home update:" + serverResponse + " to: " + getClientUsername());
        this.sendResponse(serverResponse);
    }

    @Override
    public void updateLobby(int gameId, List<String> players, int numPlayersAllowed){
        updateLobby(gameId, players, numPlayersAllowed, null);
    }

    @Override
    public void updateLobby(int gameId, List<String> players, int numPlayersAllowed, String message){
        LobbyUpdate serverResponse = new LobbyUpdate(gameId, players, numPlayersAllowed);
        if (message != null) serverResponse.setMessage(message);
        System.out.println("[SENDING] Lobby update:" + serverResponse + " to: " + getClientUsername());
        this.sendResponse(serverResponse);
    }

    @Override
    public void updateTotemAvailable(List<Totem> totemAvailable, String message){
        TotemAvailableUpdate serverResponse = new TotemAvailableUpdate(totemAvailable);
        if (message != null) serverResponse.setMessage(message);
        System.out.println("[SENDING] Lobby update:" + serverResponse + " to: " + getClientUsername());
        this.sendResponse(serverResponse);
    }


    @Override
    public void updateGame(Game game){
        updateGame(game, null);
    }

    @Override
    public void updateGame(Game game, String message){
        GameUpdate serverResponse = new GameUpdate(game.createDTO());
        if (message != null) serverResponse.setMessage(message);
        System.out.println("[SENDING] Game update:" + serverResponse + " to: " + getClientUsername());
        this.sendResponse(serverResponse);
    }

    @Override
    public void notifyError(String message){
        sendErrorMessage(message);
    }

    @Override
    public void notifyEventTriggered(String eventTitle, String logMessage){
        EventsTriggered serverResponse = new EventsTriggered(eventTitle, logMessage);
        System.out.println("[SENDING] Event triggered:" + eventTitle + " to: " + getClientUsername());
        this.sendResponse(serverResponse);
    }

    @Override
    public void notifyEndGame(int id, List<MatchResult> results, List<DBRecord> records){
        notifyEndGame(id, results, records, null);
    }

    @Override
    public void notifyEndGame(int id, List<MatchResult> results, List<DBRecord> records, String message){
        GameEnded serverResponse = new GameEnded(records, results);
        if (message != null) serverResponse.setMessage(message);
        System.out.println("[SENDING] EndGame update:" + serverResponse + " to: " + getClientUsername());
        this.sendResponse(serverResponse);
    }

    /**
     * Sends an {@link ErrorResponse} with the given message to this client.
     *
     * @param error error message to display to the client
     */
    public void sendErrorMessage(String error){
        ServerResponse serverResponse = new ErrorResponse(error);
        System.out.println("[SENDING] Home update:" + serverResponse + " to: " + getClientUsername());
        this.sendResponse(serverResponse);
    }

    /** Sends the full game-log transcript to this client only (used on reconnect). */
    public void restoreGameLog(List<String> history){
        ServerResponse serverResponse = new GameLogRestore(history);
        System.out.println("[SENDING] Game log restore (" + history.size()
                + " lines) to: " + getClientUsername());
        this.sendResponse(serverResponse);
    }

    /** Sends a {@link ServerPing} to this client to check it is still alive. */
    public void sendPing(){
        ServerResponse serverResponse = new ServerPing();
        this.sendResponse(serverResponse);
    }

    /**
     * Sends a response to the client over the transport-specific channel.
     * Must not block the calling thread for more than a brief timeout.
     *
     * @param response the response to deliver
     */
    public abstract void sendResponse(ServerResponse response);

    /**
     * Closes the underlying transport connection and releases associated resources.
     */
    public abstract void closeConnection();

    /** @return the username of this client, or empty if not yet logged in */
    public Optional<String> getClientUsername() {
        return Optional.ofNullable(clientUsername);
    }
    /** @return the ID of the game this client is currently in, or empty if none */
    public Optional<Integer> getGameId() {
        return Optional.ofNullable(gameId);
    }
    /** @return the timestamp (millis since epoch) of the last received ping */
    public long getLastPing() {
        return lastPing;
    }
    /** @return {@code true} if this client is still considered connected */
    public boolean isConnected() {
        return isConnected;
    }

    /** @param gameId ID of the game this client is now in, or {@code null} if none */
    public void setGameId(Integer gameId) {
        this.gameId = gameId;
    }
    /** @param clientUsername username to associate with this client */
    public void setClientUsername(String clientUsername) {
        this.clientUsername = clientUsername;
    }
    /** @param connected new connection status for this client */
    public void setConnected(boolean connected) {
        isConnected = connected;
    }
    /** Updates {@link #getLastPing()} to the current time. */
    public void updateLastPing() {
        this.lastPing = System.currentTimeMillis();
    }

}
