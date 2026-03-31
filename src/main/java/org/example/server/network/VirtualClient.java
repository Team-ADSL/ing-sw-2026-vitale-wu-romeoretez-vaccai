package org.example.server.network;

import org.example.server.controller.GameController;
import org.example.server.controller.ServerController;
import org.example.server.model.EndGameObserver;
import org.example.server.model.ModelObserver;
import org.example.shared.network.requests.ClientDisconnected;
import org.example.shared.network.requests.ClientRequest;

import java.util.Optional;

public abstract class VirtualClient implements ModelObserver, EndGameObserver {
    private final ServerController serverController;
    private Optional<GameController> gameController;
    private String clientUsername;

    public VirtualClient(ServerController serverController){
        this.serverController = serverController;
        this.gameController = Optional.empty();
        this.clientUsername = null;
    }

    public void processRequest(ClientRequest req){
        if(gameController.isEmpty()){
            serverController.handleClientRequest(req, this);
        } else{
            gameController.get().handleClientRequest(req, this);
        }
    }

    public void handleDisconnection() {
        ClientRequest disconnection = new ClientDisconnected(-1);
        processRequest(disconnection);
    }

    public abstract void sendErrorMessage(String error);

    public ServerController getServerController() {
        return serverController;
    }
    public Optional<GameController> getGameController() {
        return gameController;
    }
    public String getClientUsername() {
        return clientUsername;
    }

    public void setGameController(Optional<GameController> gameController) {
        this.gameController = gameController;
    }
    public void setClientUsername(String clientUsername) {
        this.clientUsername = clientUsername;
    }
}
