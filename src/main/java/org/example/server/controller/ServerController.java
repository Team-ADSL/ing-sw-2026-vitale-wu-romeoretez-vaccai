package org.example.server.controller;

import org.example.server.controller.states.LobbyState;
import org.example.server.db.GameDAO;
import org.example.server.model.Game;
import org.example.server.model.Player;
import org.example.shared.utils.Move;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class ServerController {
    private final Map<Integer, GameController> games;

    public ServerController() {
        this.games = new HashMap<>();
    }

    public synchronized void createGame(){
        try{
            int newId =  GameDAO.createMatch();
            games.put(newId, new GameController(newId, new LobbyState(null))); // Need to handle
        } catch (SQLException e){
            System.out.println(e.getMessage());
        }
    }

    public void deleteGame(int gameId){
        try{
            GameDAO.deleteMatch(gameId);
            games.remove(gameId);
        } catch (SQLException e){
            System.out.println(e.getMessage());
        }
    }

    public void saveAndQuitGame(int gameId){
        Game game = games.get(gameId).getState().getGame();
        List<String> nicknames = game.getPlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toList());
        List<Integer> scores = game.getPlayers().stream()
                .map(Player::getPp)
                .collect(Collectors.toList());
        try{
            GameDAO.saveMatch(gameId, game.getPlayers().size(), nicknames, scores);
            games.remove(gameId);
        } catch (SQLException e){
            System.out.println(e.getMessage());
        }
    }

    public void handleClientRequest(int gameId, String username, Set<Move> moves){
        if(gameId == 0){
            createGame();
        } else {
            GameController gameController = games.get(gameId);
            if(gameController != null){
                gameController.handleRequest(moves, username);
            }
        }
    }
}