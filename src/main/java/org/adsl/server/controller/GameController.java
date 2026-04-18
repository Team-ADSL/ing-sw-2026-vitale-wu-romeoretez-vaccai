package org.adsl.server.controller;

import org.adsl.server.config.BoardConfigLoader;
import org.adsl.server.controller.states.ControllerState;
import org.adsl.server.exceptions.ServerException;
import org.adsl.server.persistence.GameDAO;
import org.adsl.server.network.VirtualClient;
import org.adsl.server.persistence.GamePersistenceManager;
import org.adsl.shared.network.requests.ClientRequest;

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

    public synchronized void handleClientRequest(ClientRequest req, VirtualClient virtualClient) throws ServerException {
        req.accept(state, virtualClient);
        if(state.getNextState() != null){
            changeState(state.getNextState());
        }
    }

    // To handle automatics states
    private void changeState(ControllerState newControllerState) throws ServerException {
        ControllerState nextState = newControllerState;
        while (nextState != state && nextState != null) {
            state = nextState;
            nextState = state.onEntry();
        }
    }

    public void setState(ControllerState state) {
        this.state = state;
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
