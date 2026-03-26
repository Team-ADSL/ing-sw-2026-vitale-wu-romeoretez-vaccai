package org.example.server.controller;

import org.example.server.model.Player;
import org.example.shared.utils.Move;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ServerController {
    private final Map<Integer, GameController> games;

    public ServerController() {
        this.games = new HashMap<>();
    }

    public void createGame(){
    }

    public void handleClientRequest(int gameId, Player player, Set<Move> moves){
        GameController gameController = games.get(gameId);
        if(gameController != null){
            gameController.handleMoveRequest(moves, player);
        }
    }
}