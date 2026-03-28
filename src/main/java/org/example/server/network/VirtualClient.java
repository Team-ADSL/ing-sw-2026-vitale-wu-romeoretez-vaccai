package org.example.server.network;

import org.example.server.controller.GameController;
import org.example.server.controller.ServerController;
import org.example.server.model.ModelObserver;
import org.example.shared.network.requests.ClientRequest;

import java.util.Optional;

public abstract class VirtualClient implements ModelObserver {
    private final ServerController serverController;
    private Optional<GameController> gameController;

    public VirtualClient(ServerController serverController){
        this.serverController = serverController;
        this.gameController = Optional.empty();
    }

    public void processRequest(ClientRequest req){
        if(gameController.isPresent()) {
            gameController.get().handleClientRequest(req, this);
        } else if(req.getGameId() != 0) {
            serverController.handleClientRequest(req, this);
        }
    }

    public void setGameController(Optional<GameController> gameController) {
        this.gameController = gameController;
    }
}
