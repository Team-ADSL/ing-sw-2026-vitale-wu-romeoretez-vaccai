package org.example.server.network;

import org.example.server.controller.GameController;
import org.example.server.controller.ServerController;
import org.example.server.model.EndGameObserver;
import org.example.server.model.ModelObserver;
import org.example.shared.model.GameDTO;
import org.example.shared.model.MatchResult;
import org.example.shared.network.requests.ClientDisconnected;
import org.example.shared.network.requests.ClientRequest;
import org.example.shared.network.responses.*;

import java.util.List;
import java.util.Optional;

public abstract class VirtualClient implements ModelObserver, EndGameObserver {
    private final ServerController serverController;
    private GameController gameController;
    private String clientUsername;

    public VirtualClient(ServerController serverController){
        this.serverController = serverController;
        this.gameController = null;
        this.clientUsername = null;
    }

    public void processRequest(ClientRequest req){
        if(gameController == null){
            serverController.handleClientRequest(req, this);
        } else{
            gameController.handleClientRequest(req, this);
        }
    }

    public void handleDisconnection() {
        ClientRequest disconnection = new ClientDisconnected(-1);
        processRequest(disconnection);
    }

    // Creating and forwarding ServerResponse to the Client
    public void sendSetUsernameResponse(){
        ServerResponse serverResponse = new SetUsername();
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

    public ServerController getServerController() {
        return serverController;
    }
    public Optional<GameController> getGameController() {
        return Optional.ofNullable(gameController);
    }
    public String getClientUsername() {
        return clientUsername;
    }

    public void setGameController(GameController gameController) {
        this.gameController = gameController;
    }
    public void setClientUsername(String clientUsername) {
        this.clientUsername = clientUsername;
    }
}
