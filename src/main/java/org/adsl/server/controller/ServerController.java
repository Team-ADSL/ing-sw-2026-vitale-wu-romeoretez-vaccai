package org.adsl.server.controller;

import org.adsl.server.config.BoardConfigLoader;
import org.adsl.server.controller.states.ControllerState;
import org.adsl.server.controller.states.LobbyState;
import org.adsl.server.controller.states.RecoverState;
import org.adsl.server.network.socket.SocketServer;
import org.adsl.server.persistence.GameDAO;
import org.adsl.server.model.EndGameObserver;
import org.adsl.server.model.Game;
import org.adsl.server.model.Home;
import org.adsl.server.network.VirtualClient;
import org.adsl.server.persistence.GamePersistenceManager;
import org.adsl.server.exceptions.ServerException;
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


    public ServerController(Home home, GameDAO gameDAO, BoardConfigLoader boardConfigLoader, GamePersistenceManager gamePersistenceManager,
                            Registry registry, RemoteServerService rmiServer, SocketServer socketServer) {
        this.games = new ConcurrentHashMap<>();
        this.home = home;
        this.gameDAO = gameDAO;
        this.boardConfigLoader = boardConfigLoader;
        this.gamePersistenceManager = gamePersistenceManager;
        this.userConnected = new ConcurrentHashMap<>();
        this.timeoutScheduler = null;
        this.registry = registry;
        this.rmiServer = rmiServer;
        this.socketServer = socketServer;
    }

    public void startTimeoutChecker(int pingRatioMs, long clientTimeoutMs) {
        timeoutScheduler = Executors.newSingleThreadScheduledExecutor();
        timeoutScheduler.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            for (VirtualClient client : userConnected.values()) {
                if (now - client.getLastPing() > clientTimeoutMs) {
                    System.out.println("[NETWORK] Client timeout: " + client.getClientUsername());
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
            req.accept(this, virtualClient);
        } catch (ServerException e) {
            System.out.println("ERROR: " + e.getMessage());
            virtualClient.sendErrorMessage(e.getMessage());
        }
    }

    public void pingRoutine(VirtualClient virtualClient){
        virtualClient.updateLastPing();
        virtualClient.sendPing();
    }

    @Override
    public void visit(ClientPing req, VirtualClient virtualClient) throws ServerException {
        pingRoutine(virtualClient);

        System.out.println("[PING] " + virtualClient.getClientUsername());
    }

    @Override
    public void visit(ClientConnection req, VirtualClient virtualClient) throws ServerException {
        pingRoutine(virtualClient);

        virtualClient.sendLoginNeededResponse();
        System.out.println("[CONNECTION] " + virtualClient.getClientUsername());
    }

    @Override
    public void visit(LoginRequest req, VirtualClient virtualClient) throws ServerException {
        pingRoutine(virtualClient);

        String username = req.getUsername();

        if (username == null || username.trim().isEmpty()) {
            throw new ServerException("[LOGIN REQUEST] Invalid username provided");
        }

        VirtualClient existingClient = userConnected.putIfAbsent(username, virtualClient);

        if (existingClient != null) {
            throw new ServerException("[LOGIN REQUEST] User already connected");
        }

        virtualClient.setClientUsername(username);
        home.addObserver(virtualClient);
        home.update();
        System.out.println("[LOGIN] User connected: " + username);
    }

    @Override
    public void visit(LogoutRequest req, VirtualClient virtualClient) throws ServerException {
        pingRoutine(virtualClient);

        logout(req, virtualClient);
        System.out.println("[LOGOUT] " + virtualClient.getClientUsername());
    }

    public void controlIfLogged(VirtualClient virtualClient) throws ServerException {
        if(virtualClient.getClientUsername().isEmpty()){
            throw new ServerException("[REQUEST] User is not logged");
        }
    }

    @Override
    public void visit(CreateGameRequest req, VirtualClient virtualClient) throws ServerException {
        pingRoutine(virtualClient);

        controlIfLogged(virtualClient);
        try{
            if(req.getNumPlayer() < 2 || req.getNumPlayer() > 5 ){
                throw new ServerException("[CREATE GAME REQUEST] invalid number of players. " +
                        "Minimum players: 2; Maximum players: 5.");
            }

            int newId =  gameDAO.createMatch();
            Game newGame = new Game(newId, req.getNumPlayer());
            newGame.addObserver(this);

            GameController newGameController = new GameController(boardConfigLoader, gamePersistenceManager, gameDAO);
            newGameController.setState(new LobbyState(newGame, newGameController));
            games.put(newId, newGameController);

            EnterGameRequest newReq = new EnterGameRequest(newId);
            newGameController.handleClientRequest(newReq, virtualClient);
            home.removeObserver(virtualClient);
            home.addGame(newId);
            home.update();
            System.out.println("[CREATE] Game created by " + virtualClient.getClientUsername());
        } catch(ServerException e) {
            throw e;
        } catch (Exception e){
            throw new ServerException("[CREATE GAME] Creation failed, retry."); // Fail in database query
        }
    }

    @Override
    public void visit(EnterGameRequest req, VirtualClient virtualClient) throws ServerException {
        pingRoutine(virtualClient);

        controlIfLogged(virtualClient);
        GameController reqGame = games.get(req.getGameId());
        if(reqGame != null){
            reqGame.handleClientRequest(req, virtualClient);
            home.removeObserver(virtualClient);
        } else {
            throw new ServerException("[ENTER GAME REQUEST] The requested game does not exists.");
        }
    }

    @Override
    public void visit(ClientDisconnected req, VirtualClient virtualClient) throws ServerException {
        logout(req, virtualClient);
        virtualClient.setGameId(null);
        virtualClient.setConnected(false);
        virtualClient.closeConnection();
    }

    public void logout(ClientRequest req, VirtualClient virtualClient){
        if(virtualClient.getClientUsername().isEmpty()) {
            return;
        }
        userConnected.remove(virtualClient.getClientUsername().get());
        System.out.println("[DISCONNECTION] Removing " + virtualClient.getClientUsername());
        home.removeObserver(virtualClient);
        Optional<Integer> gameID = virtualClient.getGameId();
        if(gameID.isPresent()){
            GameController gc = games.get(gameID.get());
            if(gc != null){
                gc.handleClientRequest(req, virtualClient);
            }
        }
        virtualClient.setClientUsername(null);
    }

    @Override
    public void visit(StartGameRequest req, VirtualClient virtualClient) throws ServerException {
        pingRoutine(virtualClient);

        controlIfLogged(virtualClient);
        if(virtualClient.getGameId().isEmpty()){
            throw new ServerException("[START GAME REQUEST] Virtual client has no gameId associated.");
        }
        sendToGameController(virtualClient.getGameId().get(), req, virtualClient);
    }

    @Override
    public void visit(MoveRequest req, VirtualClient virtualClient) throws ServerException {
        pingRoutine(virtualClient);

        controlIfLogged(virtualClient);
        if(virtualClient.getGameId().isEmpty()) {
            throw new ServerException("[MOVE REQUEST] Virtual client has no gameId associated.");
        }
        sendToGameController(virtualClient.getGameId().get(), req, virtualClient);
    }

    @Override
    public void visit(ExitLobbyRequest req, VirtualClient virtualClient) throws ServerException {
        pingRoutine(virtualClient);

        controlIfLogged(virtualClient);
        if(virtualClient.getGameId().isEmpty()){
            throw new ServerException("[EXIT LOBBY REQUEST] Virtual client has no gameId associated.");
        }
        sendToGameController(virtualClient.getGameId().get(), req, virtualClient);
        home.addObserver(virtualClient);
        home.update();
    }

    public void sendToGameController(int gameId, ClientRequest req, VirtualClient virtualClient) throws ServerException {
        GameController gameController = games.get(gameId);
        if(gameController != null){
            gameController.handleClientRequest(req, virtualClient);
        } else {
            throw new ServerException("[GAME REQUEST] Game requested does not exists.");
        }
    }

    @Override
    public void notifyEndGame(int gameId, List<MatchResult> matchResults) {
        games.remove(gameId);

        home.removeGame(gameId);
        List<VirtualClient> newClientInHome = userConnected.values().stream()
                .filter(c -> c.getGameId().isPresent())
                .filter(c -> c.getGameId().get() == gameId)
                .toList();
        for(VirtualClient c : newClientInHome){
            home.addObserver(c);
            c.setGameId(null);
        }
        home.update();

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
                System.out.println("ERROR: [DB] Some problem during deletion of game "
                        + gameId + "\n"
                        + e.getMessage());
            }
        }
    }

    public void recoverGames(){
        try{
            List<Game> gamesLoaded = gamePersistenceManager.recoverGames();
            gamesLoaded.forEach(g -> {
                g.getPlayers().forEach(p -> p.setActive(false));
                g.setupTransientAttributes();
                g.addObserver(this);
                GameController gc = new GameController(boardConfigLoader, gamePersistenceManager, gameDAO);
                games.put(g.getGameId(), gc);
                home.addGame(g.getGameId());
                ControllerState state = new RecoverState(g, gc);
                gc.setState(state);
            });
            home.update();
        } catch(Exception e){
            System.out.println("ERROR: [RECOVER] Some problem while reading " +
                    "game saved.\n" + e.getMessage());
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
                System.out.println("[CLOSING] GameServer removed from Registry.");
            } catch (NotBoundException | RemoteException e) {
                System.out.println("ERROR: [CLOSING] RMI service already removed.");
            }
        }

        try {
            if (rmiServer != null) {
                UnicastRemoteObject.unexportObject(this.rmiServer, true);
                System.out.println("[CLOSING] Removed rmiServer object.");
            }

            if (this.registry != null) {
                UnicastRemoteObject.unexportObject(this.registry, true);
                System.out.println("[CLOSING] Registry stopped.");
            }
        } catch (NoSuchObjectException e) {
            throw new RuntimeException(e);
        }
    }
}