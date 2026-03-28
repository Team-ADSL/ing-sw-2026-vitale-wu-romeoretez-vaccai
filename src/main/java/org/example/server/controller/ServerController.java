package org.example.server.controller;

import org.example.server.controller.states.LobbyControllerState;
import org.example.server.db.GameDAO;
import org.example.server.model.Game;
import org.example.server.model.Lobby;
import org.example.server.model.Player;
import org.example.server.network.VirtualClient;
import org.example.shared.exceptions.InvalidMoveException;
import org.example.shared.network.RequestVisitor;
import org.example.shared.network.requests.*;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class ServerController implements RequestVisitor<VirtualClient> {
    private final Map<Integer, GameController> games;
    private final Lobby lobby;

    public ServerController() {
        this.games = new HashMap<>();
        this.lobby = new Lobby();
    }

    public void handleClientRequest(ClientRequest req, VirtualClient virtualClient){
        try {
            req.accept(this, virtualClient);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            lobby.updateAll(e.getMessage());
        }
    }

    @Override
    public void visit(CreateGameRequest req, VirtualClient virtualClient) throws InvalidMoveException {
        // Implement
    }
    @Override
    public void visit(ConnectToGameRequest req, VirtualClient virtualClient) throws InvalidMoveException {
        // Implement
    }
    @Override
    public void visit(ClientDisconnected req, VirtualClient virtualClient) throws InvalidMoveException {
        // Implement
    }
    @Override
    public void visit(StartGameRequest req, VirtualClient virtualClient) throws InvalidMoveException {
        throw new InvalidMoveException("Move not allowed in this phase");
    }
    @Override
    public void visit(MakeMoveRequest req, VirtualClient virtualClient) throws InvalidMoveException {
        throw new InvalidMoveException("Move not allowed in this phase");
    }

    public synchronized GameController createGame(){
        try{
            int newId =  GameDAO.createMatch();
            GameController newGameController = new GameController(newId, new LobbyControllerState(null));
            games.put(newId, newGameController); // Need to handle
            return newGameController;
        } catch (SQLException e){
            System.out.println(e.getMessage());
            return null;
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
}