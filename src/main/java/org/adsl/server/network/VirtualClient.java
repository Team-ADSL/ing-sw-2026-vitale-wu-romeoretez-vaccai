package org.adsl.server.network;

import org.adsl.server.controller.ServerController;
import org.adsl.server.model.EndGameObserver;
import org.adsl.server.model.Game;
import org.adsl.server.model.GameObserver;
import org.adsl.shared.model.MatchResult;
import org.adsl.shared.network.requests.ClientConnection;
import org.adsl.shared.network.requests.ClientDisconnected;
import org.adsl.shared.network.requests.ClientRequest;
import org.adsl.shared.network.responses.*;

import java.util.List;
import java.util.Optional;

public abstract class VirtualClient implements GameObserver, HomeObserver, EndGameObserver {
    private final ServerController serverController;
    private String clientUsername;
    private Integer gameId;
    private boolean isConnected;
    private volatile long lastPing;

    public VirtualClient(ServerController serverController){
        this.serverController = serverController;
        this.clientUsername = null;
        this.isConnected = true;
        this.gameId = null;
        this.lastPing = System.currentTimeMillis();
    }

    public void processRequest(ClientRequest req){
        if(isConnected){
            serverController.handleClientRequest(req, this);
        }
    }

    public void handleDisconnection() {
        if(isConnected){
            setConnected(false);

            System.out.println("[NETWORK] Disconnection initiated for: " + getClientUsername());
            ClientRequest disconnection = new ClientDisconnected();

            serverController.handleClientRequest(disconnection, this);
        }
    }

    public void handleConnection() {
        System.out.println("[NETWORK] New connection established.");
        ClientConnection connection = new ClientConnection();
        processRequest(connection);
    }

    // Creating and forwarding ServerResponse to the Client
    public void sendLoginNeededResponse(){
        ServerResponse serverResponse = new LoginNeeded();
        this.sendResponse(serverResponse);
    }

    @Override
    public void updateHome(List<Integer> activeGames){
        ServerResponse serverResponse = new HomeUpdate(activeGames);
        System.out.println("[SENDING] Home update:" + serverResponse + " to: " + getClientUsername());
        this.sendResponse(serverResponse);
    }

    @Override
    public void updateLobby(int gameId, List<String> players, int numPlayersAllowed){
        ServerResponse serverResponse = new LobbyUpdate(gameId, players, numPlayersAllowed);
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
    public void notifyEndGame(int id, List<MatchResult> results){
        ServerResponse serverResponse = new GameEnded(results);
        System.out.println("[SENDING] EndGame update:" + serverResponse + " to: " + getClientUsername());
        this.sendResponse(serverResponse);
    }

    public void sendErrorMessage(String error){
        ServerResponse serverResponse = new ErrorResponse(error);
        System.out.println("[SENDING] Home update:" + serverResponse + " to: " + getClientUsername());
        this.sendResponse(serverResponse);
    }

    public void sendPing(){
        ServerResponse serverResponse = new ServerPing();
        this.sendResponse(serverResponse);
    }

    public abstract void sendResponse(ServerResponse response);
    public abstract void closeConnection();

    public Optional<String> getClientUsername() {
        return Optional.ofNullable(clientUsername);
    }
    public Optional<Integer> getGameId() {
        return Optional.ofNullable(gameId);
    }
    public long getLastPing() {
        return lastPing;
    }
    public boolean isConnected() {
        return isConnected;
    }

    public void setGameId(Integer gameId) {
        this.gameId = gameId;
    }
    public void setClientUsername(String clientUsername) {
        this.clientUsername = clientUsername;
    }
    public void setConnected(boolean connected) {
        isConnected = connected;
    }
    public void updateLastPing() {
        this.lastPing = System.currentTimeMillis();
    }

}
