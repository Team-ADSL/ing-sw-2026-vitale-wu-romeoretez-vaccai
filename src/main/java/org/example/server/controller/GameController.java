package org.example.server.controller;

import org.example.server.controller.states.ControllerState;
import org.example.server.controller.states.LobbyState;
import org.example.server.controller.states.RecoverState;
import org.example.server.model.Game;
import org.example.server.config.BoardConfigLoader;
import org.example.server.persistence.GameDAO;
import org.example.server.network.VirtualClient;
import org.example.server.persistence.GamePersistenceManager;
import org.example.shared.exceptions.InvalidRequestException;
import org.example.shared.network.requests.ClientRequest;


public class GameController {
    private ControllerState state;
    private final BoardConfigLoader boardConfigLoader;
    private final GamePersistenceManager persistenceManager;
    private final GameDAO gameDAO;

    public GameController(Game game, BoardConfigLoader boardConfigLoader,
                          GamePersistenceManager persistenceManager, GameDAO gameDAO) {
        this.state = new LobbyState(game, this);
        this.boardConfigLoader = boardConfigLoader;
        this.persistenceManager = persistenceManager;
        this.gameDAO = gameDAO;
    }

    public GameController(int gameId, BoardConfigLoader boardConfigLoader,
                          GamePersistenceManager persistenceManager, GameDAO gameDAO) {
        this.state = new RecoverState(null, this, gameId);
        this.boardConfigLoader = boardConfigLoader;
        this.persistenceManager = persistenceManager;
        this.gameDAO = gameDAO;
    }

    public synchronized void handleClientRequest(ClientRequest req, VirtualClient virtualClient){
        try {
            req.accept(state, virtualClient);
            ControllerState newState = state.nextState();
            if(newState != null){
                changeState(newState);
            }
        } catch(InvalidRequestException e){
            System.out.println(e.getMessage());
            virtualClient.sendErrorMessage(e.getMessage());
        } catch(Exception e){
            System.out.println(e.getMessage());
        }
    }

    // To handle automatics states
    private void changeState(ControllerState newControllerState) throws Exception{
        ControllerState nextState = newControllerState;
        while (nextState != state && nextState != null) {
            state = nextState;
            nextState = state.onEntry();
        }
    }

    public ControllerState getState() {
        return state;
    }
    public BoardConfigLoader getBoardConfigLoader() {
        return boardConfigLoader;
    }
    public GamePersistenceManager getPersistenceManager() {
        return persistenceManager;
    }
    public GameDAO getGameDAO() {
        return gameDAO;
    }
}
