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
import org.example.shared.exceptions.InvalidRequestException;
import org.example.shared.model.MatchResult;
import org.example.shared.network.requests.RequestVisitor;
import org.example.shared.network.requests.*;

import java.util.*;

public class ServerController implements RequestVisitor<VirtualClient>, EndGameObserver {
    private final Map<Integer, GameController> games;
    private final Home home;
    private final GameDAO gameDAO;
    private final BoardConfigLoader boardConfigLoader;
    private final GamePersistenceManager gamePersistenceManager;

    public ServerController(GameDAO gameDAO, BoardConfigLoader boardConfigLoader, GamePersistenceManager gamePersistenceManager) {
        this.games = new HashMap<>();
        this.home = new Home();
        this.gameDAO = gameDAO;
        this.boardConfigLoader = boardConfigLoader;
        this.gamePersistenceManager = gamePersistenceManager;
    }

    public void handleClientRequest(ClientRequest req, VirtualClient virtualClient){
        try {
            req.accept(this, virtualClient);
            home.update();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            virtualClient.sendErrorMessage(e.getMessage());
        }
    }

    @Override
    public void visit(ClientConnection req, VirtualClient virtualClient) throws InvalidRequestException {
        // Handle initial connection
        home.update();
    }
    @Override
    public void visit(SetUsernameRequest req, VirtualClient virtualClient) throws InvalidRequestException {
        // Setting of username
    }
    @Override
    public void visit(CreateGameRequest req, VirtualClient virtualClient) throws InvalidRequestException {
        createGame(req, virtualClient);
    }
    @Override
    public void visit(EnterGameRequest req, VirtualClient virtualClient) throws InvalidRequestException {
        connectToGame(req, virtualClient);
    }
    @Override
    public void visit(ClientDisconnected req, VirtualClient virtualClient) throws InvalidRequestException {
        // Handle disconnection
    }
    @Override
    public void visit(StartGameRequest req, VirtualClient virtualClient) throws InvalidRequestException {
        throw new InvalidRequestException("Server received invalid request");
    }
    @Override
    public void visit(MakeMoveRequest req, VirtualClient virtualClient) throws InvalidRequestException {
        throw new InvalidRequestException("Server received invalid request");
    }

    @Override
    public void notifyEndGame(int gameId, List<MatchResult> matchResults) {
        synchronized (games){
            games.remove(gameId);
        }
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

    public void createGame(CreateGameRequest req, VirtualClient virtualClient){
        try{
            int newId =  gameDAO.createMatch();
            Game newGame = new Game(newId, req.getNumPlayer(), this);
            GameController newGameController = new GameController(boardConfigLoader, gamePersistenceManager,gameDAO);
            newGameController.setState(new InitGameState(newGame, newGameController));
            synchronized (games){
                games.put(newId, newGameController);
            }
            EnterGameRequest newReq = new EnterGameRequest(newId);
            newGameController.handleClientRequest(newReq, virtualClient);
        } catch (Exception e){
            System.out.println(e.getMessage());
            virtualClient.sendErrorMessage(e.getMessage());
        }
    }

    public void connectToGame(EnterGameRequest req, VirtualClient virtualClient){
        try{
            if(games.containsKey(req.getGameId())){
                GameController reqGame = games.get(req.getGameId());
                reqGame.handleClientRequest(req, virtualClient);
            } else {
                throw new InvalidRequestException("The requested game does not exists");
            }
        } catch (Exception e){
            System.out.println(e.getMessage());
            virtualClient.sendErrorMessage(e.getMessage());
        }
    }

    public void recoverGames(){
        try{
            List<Game> gamesLoaded = gamePersistenceManager.recoverGames();
            gamesLoaded.forEach(g -> {
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