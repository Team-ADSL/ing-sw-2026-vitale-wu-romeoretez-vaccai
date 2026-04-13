package org.example.server.network;

import org.example.server.controller.ServerController;
import org.example.server.model.EndGameObserver;
import org.example.server.model.GameObserver;
import org.example.shared.model.GameDTO;
import org.example.shared.model.MatchResult;
import org.example.shared.network.requests.ClientConnection;
import org.example.shared.network.requests.ClientDisconnected;
import org.example.shared.network.requests.ClientRequest;
import org.example.shared.network.responses.*;

import java.util.List;
import java.util.Optional;

public abstract class VirtualClient implements GameObserver, HomeObserver, EndGameObserver {
    private final ServerController serverController;
    private String clientUsername;
    private Integer gameId;
    private boolean isConnected;

    public VirtualClient(ServerController serverController){
        this.serverController = serverController;
        this.clientUsername = null;
        this.isConnected = false;
        this.gameId = null;
    }

    public void processRequest(ClientRequest req){
        serverController.handleClientRequest(req, this);
    }

    public void handleDisconnection() {
        if(!isConnected){
            return;
        }
        ClientRequest disconnection = new ClientDisconnected();
        processRequest(disconnection);
    }

    public void handleConnection() {
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
        this.sendResponse(serverResponse);
    }

    @Override
    public void updateLobby(List<String> players){
        ServerResponse serverResponse = new LobbyUpdate(players);
        this.sendResponse(serverResponse);
    }

    @Override
    public void updateGame(GameDTO game){
        ServerResponse serverResponse = new GameUpdate(game);
        this.sendResponse(serverResponse);
    }

    @Override
    public void notifyEndGame(int id, List<MatchResult> results){
        ServerResponse serverResponse = new GameEnded(results);
        this.sendResponse(serverResponse);
    }

    public void sendErrorMessage(String error){
        ServerResponse serverResponse = new ErrorResponse(error);
        this.sendResponse(serverResponse);
    }

    public abstract void sendResponse(ServerResponse response);

    public Optional<String> getClientUsername() {
        return Optional.ofNullable(clientUsername);
    }
    public Optional<Integer> getGameId() {
        return Optional.ofNullable(gameId);
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
}
