package org.adsl.server.controller;

import org.adsl.server.config.BoardConfigLoader;
import org.adsl.server.controller.states.ControllerState;
import org.adsl.server.controller.states.InitGameState;
import org.adsl.server.controller.states.RecoverState;
import org.adsl.server.network.socket.SocketServer;
import org.adsl.server.persistence.GameDAO;
import org.adsl.server.model.EndGameObserver;
import org.adsl.server.model.Game;
import org.adsl.server.model.Home;
import org.adsl.server.network.VirtualClient;
import org.adsl.server.persistence.GamePersistenceManager;
import org.adsl.server.exceptions.GameException;
import org.adsl.shared.model.MatchResult;
import org.adsl.shared.network.remote.RemoteServerService;
import org.adsl.shared.network.requests.RequestVisitor;
import org.adsl.shared.network.requests.*;

import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ServerController implements RequestVisitor<VirtualClient>, EndGameObserver {
    private final Map<Integer, GameController> games;
    private final Map<String, VirtualClient> userConnected;
    private final Home home;

    private final GameDAO gameDAO;
    private final BoardConfigLoader boardConfigLoader;
    private final GamePersistenceManager gamePersistenceManager;

    private ScheduledExecutorService timeoutScheduler;
    private final Registry registry;
    private final RemoteServerService rmiServer;
    private final SocketServer socketServer;


    public ServerController(GameDAO gameDAO, BoardConfigLoader boardConfigLoader, GamePersistenceManager gamePersistenceManager,
                            Registry registry, RemoteServerService rmiServer, SocketServer socketServer) {
        this.games = new ConcurrentHashMap<>();
        this.home = new Home();
        this.gameDAO = gameDAO;
        this.boardConfigLoader = boardConfigLoader;
        this.gamePersistenceManager = gamePersistenceManager;
        this.userConnected = new ConcurrentHashMap<>();
        this.timeoutScheduler = null;
        this.registry = registry;
        this.rmiServer = rmiServer;
        this.socketServer = socketServer;
    }

    public void startTimeoutChecker(int pingRatioMs, long clientTimoutMs) {
        timeoutScheduler = Executors.newSingleThreadScheduledExecutor();
        timeoutScheduler.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            for (VirtualClient client : userConnected.values()) {
                if (now - client.getLastPing() > clientTimoutMs) {
                    System.out.println("Client timeout: " + client.getClientUsername());
                    client.handleDisconnection();
                }
            }
        }, 0, pingRatioMs, TimeUnit.MILLISECONDS);
    }

    public void stopTimeoutChecker() {
        if (timeoutScheduler != null) {
            timeoutScheduler.shutdownNow();
        }
    }

    public void handleClientRequest(ClientRequest req, VirtualClient virtualClient){
        try {
            virtualClient.updateLastPing();
            req.accept(this, virtualClient);
            home.update();
        } catch (GameException e) {
            System.out.println(e.getMessage());
            virtualClient.sendErrorMessage(e.getMessage());
        }
    }

    @Override
    public void visit(ClientPing req, VirtualClient virtualClient) throws GameException {
        virtualClient.sendPing();
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
                throw new GameException("Minimum players: 2; Maximum players: 5.");
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
        } catch (Exception e){
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
        if(virtualClient.getClientUsername().isPresent()) {
            userConnected.remove(virtualClient.getClientUsername().get());
        }
        home.removeObserver(virtualClient);
        Optional<Integer> gameID = virtualClient.getGameId();
        if(gameID.isPresent()){
            GameController gc = games.get(gameID.get());
            if(gc != null){
                gc.handleClientRequest(req, virtualClient);
            }
        }
        virtualClient.setGameId(null);
        virtualClient.setConnected(false);
        virtualClient.closeConnection();
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

    public void shutdown(){
        stopTimeoutChecker();
        shutdownRMI();
        socketServer.shutdown();
    }

    public void shutdownRMI(){
        if (this.registry != null) {
            try {
                registry.unbind("GameServer");
                System.out.println("GameServer removed from Registry.");
            } catch (NotBoundException | RemoteException e) {
                System.out.println("RMI service already removed.");
            }
        }

        try {
            if (rmiServer != null) {
                UnicastRemoteObject.unexportObject(this.rmiServer, true);
                System.out.println("Removed rmiServer object.");
            }

            if (this.registry != null) {
                UnicastRemoteObject.unexportObject(this.registry, true);
                System.out.println("Registry stopped.");
            }
        } catch (NoSuchObjectException e) {
            throw new RuntimeException(e);
        }
    }
}