package org.adsl.server.controller;

import org.adsl.server.config.BoardConfigLoader;
import org.adsl.server.controller.states.ControllerState;
import org.adsl.server.controller.states.LobbyState;
import org.adsl.server.controller.states.RecoverState;
import org.adsl.shared.exceptions.HostDisconnectedException;
import org.adsl.server.network.socket.SocketServer;
import org.adsl.server.persistence.GameDAO;
import org.adsl.server.model.EndGameObserver;
import org.adsl.server.model.Game;
import org.adsl.server.model.Home;
import org.adsl.server.network.VirtualClient;
import org.adsl.server.persistence.GamePersistenceManager;
import org.adsl.shared.exceptions.ServerException;
import org.adsl.shared.model.DBRecord;
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

/**
 * Top-level server controller. Manages all active games and all connected clients.
 * <p>
 * Implements {@link RequestVisitor} to handle lobby-level requests (login, logout,
 * game creation/join). Game-specific requests are forwarded to the appropriate
 * {@link GameController}. A periodic timeout checker pings clients every
 * {@code pingRatioMs} ms and disconnects those silent for more than
 * {@code clientTimeoutMs} ms.
 * </p>
 */
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

    /**
     * Starts the background timeout checker on a single-threaded scheduler.
     * Clients that have not pinged within {@code clientTimeoutMs} ms are
     * forcefully disconnected.
     *
     * @param pingRatioMs    polling interval in milliseconds
     * @param clientTimeoutMs inactivity threshold in milliseconds
     */
    public void startTimeoutChecker(int pingRatioMs, long clientTimeoutMs) {
        timeoutScheduler = Executors.newSingleThreadScheduledExecutor();
        timeoutScheduler.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            for (VirtualClient client : userConnected.values()) {
                if (now - client.getLastPing() > clientTimeoutMs) {
                    System.out.println("[NETWORK] Client timeout: " + client.getClientUsername());
                    client.handleDisconnection();
                } else if (client.isConnected()) {
                    // Active liveness probe. RMI has no server-side listener
                    // thread; without an unsolicited push, a dead RMI client
                    // is only detected when clientTimeoutMs elapses. Pushing
                    // a ServerPing forces clientStub.sendResponse() to throw
                    // RemoteException synchronously when the client is gone,
                    // and the catch in RMIClientHandler.sendResponse then
                    // calls handleDisconnection() — matching the immediacy
                    // of the socket listener thread. On Socket the push is
                    // a no-op (PrintWriter swallows I/O); the listener
                    // remains authoritative for socket-side drops.
                    client.sendPing();
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
        } catch (RuntimeException e) {

            System.err.println("FATAL: unchecked exception while handling "
                    + req.getClass().getSimpleName() + " from "
                    + virtualClient.getClientUsername()
                    + " : " + e.getMessage());
            virtualClient.sendErrorMessage("Internal server error: " + e.getMessage());
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
        String log = "[LOGIN] User connected: " + username;
        System.out.println(log);
        home.update(buildGamePlayersMap(), buildGameCapacityMap(), log);

        int gameWithPlayer = games.values().stream()
                .map(gc -> gc.getState().getGame())
                .filter(g -> g.getPlayers().stream().anyMatch(p -> p.getName().equals(username)))
                .map(Game::getGameId)
                .findFirst()
                .orElse(0);

        if(gameWithPlayer != 0){
            sendToGameController(gameWithPlayer, new EnterGameRequest(gameWithPlayer), virtualClient);

        } else {
            home.addObserver(virtualClient);
            home.update();
        }
    }

    @Override
    public void visit(LogoutRequest req, VirtualClient virtualClient) throws ServerException {
        pingRoutine(virtualClient);

        logout(req, virtualClient);
        virtualClient.sendLoginNeededResponse();
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
            String host = virtualClient.getClientUsername().orElseThrow(() ->
                    new ServerException("[CREATE GAME REQUEST] Client not connected"));
            newGameController.setState(new LobbyState(newGame, newGameController, host));
            games.put(newId, newGameController);

            EnterGameRequest newReq = new EnterGameRequest(newId);
            newGameController.handleClientRequest(newReq, virtualClient);
            home.removeObserver(virtualClient);
            home.addGame(newId);
            String log = "[CREATE] Game created by " + virtualClient.getClientUsername();
            System.out.println(log);
            home.update(buildGamePlayersMap(), buildGameCapacityMap(), log);
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
        int gameId = virtualClient.getGameId().orElse(-1);
        try {
            logout(req, virtualClient);
        } catch(HostDisconnectedException e){
            handleHostDisconnection(gameId);
        }
        virtualClient.closeConnection();
    }

    public void logout(ClientRequest req, VirtualClient virtualClient) throws ServerException{
        if(virtualClient.getClientUsername().isEmpty()) {
            return;
        }
        userConnected.remove(virtualClient.getClientUsername().get());
        System.out.println("[LOGOUT] Removing " + virtualClient.getClientUsername());
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
    public void visit(TotemPickingRequest req, VirtualClient virtualClient) throws ServerException {
        pingRoutine(virtualClient);

        controlIfLogged(virtualClient);
        if(virtualClient.getGameId().isEmpty()) {
            throw new ServerException("[TOTEM PICKING REQUEST] Virtual client has no gameId associated.");
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

        int gameId = virtualClient.getGameId().get();
        try {
            sendToGameController(gameId, req, virtualClient);
        } catch(HostDisconnectedException e){
            handleHostDisconnection(gameId);
        }
        home.addObserver(virtualClient);
        home.update(buildGamePlayersMap(), buildGameCapacityMap());
    }

    /**
     * Evicts all clients still in the lobby of the given game when the host
     * disconnects. Each evicted client is moved back to the home screen.
     *
     * @param gameId the game whose host has disconnected
     * @throws ServerException if forwarding the exit request to the game fails
     */
    public void handleHostDisconnection(int gameId) throws ServerException{
        List<VirtualClient> clientToDisconnect = userConnected.values().stream()
                .filter(c -> c.getGameId().orElse(-1) == gameId)
                .toList();
        for(VirtualClient c : clientToDisconnect){
            sendToGameController(gameId, new ExitLobbyRequest(), c);
            home.addObserver(c);
        }
    }

    @Override
    public void visit(ExitGameRequest req, VirtualClient virtualClient) throws ServerException {
        pingRoutine(virtualClient);

        controlIfLogged(virtualClient);
        if(virtualClient.getGameId().isEmpty()){
            throw new ServerException("[EXIT GAME REQUEST] Virtual client has no gameId associated.");
        }
        
        home.addObserver(virtualClient);
        virtualClient.setGameId(null);
        home.update(buildGamePlayersMap(), buildGameCapacityMap());
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
    public void notifyEndGame(int gameId, List<MatchResult> results,  List<DBRecord> records) {
        games.remove(gameId);

        home.removeGame(gameId);
        try {
            gamePersistenceManager.removeGame(gameId);
        } catch(Exception e){
            System.out.println(e.getMessage());
        }

        // If game terminates before starting we clean the database
        if(results == null){
            try {
                gameDAO.deleteMatch(gameId);
            } catch(Exception e){
                System.out.println("ERROR: [DB] Some problem during deletion of game "
                        + gameId + "\n"
                        + e.getMessage());
            }
        }
    }

    /**
     * Loads persisted game states from disk and re-registers them as
     * {@link RecoverState} instances, waiting for players to reconnect.
     * Called once at server startup.
     */
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
            home.update(buildGamePlayersMap(), buildGameCapacityMap());
        } catch(Exception e){
            System.out.println("ERROR: [RECOVER] Some problem while reading " +
                    "game saved.\n" + e.getMessage());
        }
    }

    private Map<Integer, List<String>> buildGamePlayersMap() {
        Map<Integer, List<String>> map = new HashMap<>();
        for (Map.Entry<Integer, GameController> entry : games.entrySet()) {
            map.put(entry.getKey(), entry.getValue().getLobbyPlayers());
        }
        return map;
    }

    private Map<Integer, Integer> buildGameCapacityMap() {
        Map<Integer, Integer> map = new HashMap<>();
        for (Map.Entry<Integer, GameController> entry : games.entrySet()) {
            int cap = entry.getValue().getCapacity();
            if (cap > 0) map.put(entry.getKey(), cap);
        }
        return map;
    }

    /**
     * @return maxValue from the recovered games
     */
    public int getMaxGameId(){
        return games.keySet().stream()
                .mapToInt(id -> id)
                .max()
                .orElse(0);
    }

    /**
     * Gracefully shuts down the server: stops the timeout checker, unbinds
     * RMI registry entries, unexports RMI objects, and stops the socket server.
     */
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