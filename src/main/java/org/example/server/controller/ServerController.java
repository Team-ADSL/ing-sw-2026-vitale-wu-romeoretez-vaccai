package org.example.server.controller;

import org.example.server.config.BoardConfigLoader;
import org.example.server.controller.states.ControllerState;
import org.example.server.controller.states.InitGameState;
import org.example.server.controller.states.RecoverState;
import org.example.server.persistence.GameDAO;
import org.example.server.model.EndGameObserver;
import org.example.server.model.Game;
import org.example.server.model.Home;
import org.example.server.network.VirtualClient;
import org.example.server.persistence.GamePersistenceManager;
import org.example.server.exceptions.GameException;
import org.example.shared.model.MatchResult;
import org.example.shared.network.requests.RequestVisitor;
import org.example.shared.network.requests.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ServerController implements RequestVisitor<VirtualClient>, EndGameObserver {
    private final Map<Integer, GameController> games;
    private final Map<String, VirtualClient> userConnected;
    private final Home home;
    private final GameDAO gameDAO;
    private final BoardConfigLoader boardConfigLoader;
    private final GamePersistenceManager gamePersistenceManager;

    public ServerController(GameDAO gameDAO, BoardConfigLoader boardConfigLoader, GamePersistenceManager gamePersistenceManager) {
        this.games = new ConcurrentHashMap<>();
        this.home = new Home();
        this.gameDAO = gameDAO;
        this.boardConfigLoader = boardConfigLoader;
        this.gamePersistenceManager = gamePersistenceManager;
        this.userConnected = new ConcurrentHashMap<>();
    }

    public void handleClientRequest(ClientRequest req, VirtualClient virtualClient){
        try {
            req.accept(this, virtualClient);
            home.update();
        } catch (GameException e) {
            System.out.println(e.getMessage());
            virtualClient.sendErrorMessage(e.getMessage());
        }
    }

    @Override
    public void visit(ClientConnection req, VirtualClient virtualClient) throws GameException {
        virtualClient.sendLoginNeededResponse(); // Implicitly confirming connection
    }

    @Override
    public void visit(LoginRequest req, VirtualClient virtualClient) throws GameException {
        if(userConnected.containsKey(req.getUsername())){
            virtualClient.sendErrorMessage("User already connected");
        } else {
            virtualClient.setConnected(true);
            virtualClient.setClientUsername(req.getUsername());
            userConnected.put(req.getUsername(), virtualClient);
            home.addObserver(virtualClient);
            System.out.println("[LOGIN] User connected: " + req.getUsername());
        }
    }

    @Override
    public void visit(CreateGameRequest req, VirtualClient virtualClient) throws GameException {
        try{
            if(req.getNumPlayer() < 2 || req.getNumPlayer() > 5 ){
                throw new java.lang.Exception("Minimum players: 2; Maximum players: 5.");
            }

            int newId =  gameDAO.createMatch();
            Game newGame = new Game(newId, req.getNumPlayer());
            newGame.addObserver(this);

            GameController newGameController = new GameController(boardConfigLoader, gamePersistenceManager, gameDAO);
            newGameController.setState(new InitGameState(newGame, newGameController));
            games.put(newId, newGameController);

            EnterGameRequest newReq = new EnterGameRequest(newId);
            newGameController.handleClientRequest(newReq, virtualClient);
            // SEE ENTER GAME
        } catch (java.lang.Exception e){
            System.out.println(e.getMessage());
            virtualClient.sendErrorMessage(e.getMessage());
        }
    }

    @Override
    public void visit(EnterGameRequest req, VirtualClient virtualClient) throws GameException {
        try{
            if(virtualClient.getGameId().isEmpty()){
                throw new GameException("Virtual client has no gameID associated.");
            }
            GameController reqGame = games.get(virtualClient.getGameId().get());
            if(reqGame != null){
                reqGame.handleClientRequest(req, virtualClient);
                // NEED TO REMOVE FROM HOME OBSERVER ONLY IF SUCCESS,
                // CHANGE EXCEPTION HANDLING
            } else {
                throw new GameException("The requested game does not exists.");
            }
        } catch (Exception e){
            System.out.println(e.getMessage());
            virtualClient.sendErrorMessage(e.getMessage());
        }
    }

    @Override
    public void visit(ClientDisconnected req, VirtualClient virtualClient) throws GameException {
        home.removeObserver(virtualClient);
        if(virtualClient.getClientUsername().isEmpty()){
            throw new GameException("Virtual client has no username associated.");
        } else {
            Optional<Integer> gameID = virtualClient.getGameId();
            if(gameID.isPresent()){
                GameController gc = games.get(gameID.get());
                gc.handleClientRequest(req, virtualClient);
            }
            virtualClient.setConnected(false);
            userConnected.remove(virtualClient.getClientUsername().get());
        }
    }

    @Override
    public void visit(StartGameRequest req, VirtualClient virtualClient) throws GameException {
        if(virtualClient.getGameId().isEmpty()){
            throw new GameException("Virtual client has no gameID associated.");
        }
        sendToGameController(virtualClient.getGameId().get(), req, virtualClient);
    }

    @Override
    public void visit(MakeMoveRequest req, VirtualClient virtualClient) throws GameException {
        if(virtualClient.getGameId().isEmpty()) {
            throw new GameException("Virtual client has no gameID associated.");
        }
        sendToGameController(virtualClient.getGameId().get(), req, virtualClient);
    }

    public void sendToGameController(int gameId, ClientRequest req, VirtualClient virtualClient) throws GameException {
        GameController gameController = games.get(gameId);
        if(gameController != null){
            gameController.handleClientRequest(req, virtualClient);
        } else {
            throw new GameException("Game requested does not exists.");
        }
    }

    @Override
    public void notifyEndGame(int gameId, List<MatchResult> matchResults) {
        games.remove(gameId);
        try {
            gamePersistenceManager.removeGame(gameId);
        } catch(Exception e){
            System.out.println(e.getMessage());
        }
        // If game terminates before starting we clean the database
        if(matchResults == null){
            try {
                gameDAO.deleteMatch(gameId);
            } catch(Exception e){
                System.out.println(e.getMessage());
            }
        }
    }

    public void recoverGames(){
        try{
            List<Game> gamesLoaded = gamePersistenceManager.recoverGames();
            gamesLoaded.forEach(g -> {
                g.getPlayers().forEach(p -> p.setActive(false));
                g.addObserver(this);
                GameController gc = new GameController(boardConfigLoader, gamePersistenceManager, gameDAO);
                games.put(g.getGameId(), gc);
                ControllerState state = new RecoverState(g, gc);
                gc.setState(state);
            });
        } catch(Exception e){
            System.out.println(e.getMessage());
        }
    }
}