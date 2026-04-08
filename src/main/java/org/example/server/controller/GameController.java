package org.example.server.controller;

import org.example.server.config.BoardConfigLoader;
import org.example.server.controller.states.ControllerState;
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

    public GameController(BoardConfigLoader boardConfigLoader,
                          GamePersistenceManager persistenceManager, GameDAO gameDAO) {
        this.boardConfigLoader = boardConfigLoader;
        this.persistenceManager = persistenceManager;
        this.gameDAO = gameDAO;
        this.state = null;
    }

    public synchronized void handleClientRequest(ClientRequest req, VirtualClient virtualClient){
        try {
            req.accept(state, virtualClient);
            if(state.getNextState() != null){
                changeState(state.getNextState());
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

    public void setState(ControllerState state) {
        this.state = state;
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
