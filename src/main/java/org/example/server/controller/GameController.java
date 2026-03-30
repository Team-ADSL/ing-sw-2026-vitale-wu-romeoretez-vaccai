package org.example.server.controller;

import org.example.server.controller.states.ControllerState;
import org.example.server.controller.states.LobbyState;
import org.example.server.model.Game;
import org.example.server.config.BoardConfigLoader;
import org.example.server.persistence.GameDAO;
import org.example.server.network.VirtualClient;
import org.example.shared.exceptions.InvalidRequestException;
import org.example.shared.network.requests.ClientRequest;


public class GameController {
    private ControllerState state;
    private final GameDAO gameDAO;
    private final BoardConfigLoader boardConfigLoader;

    public GameController(Game game, GameDAO gameDAO, BoardConfigLoader boardConfigLoader) {
        this.state = new LobbyState(game, this);
        this.gameDAO = gameDAO;
        this.boardConfigLoader = boardConfigLoader;
    }

    public GameController(ControllerState state, GameDAO gameDAO, BoardConfigLoader boardConfigLoader) {
        this.state = state;
        this.gameDAO = gameDAO;
        this.boardConfigLoader = boardConfigLoader;
    }

    public synchronized void handleClientRequest(ClientRequest req, VirtualClient virtualClient){
        try {
            req.accept(state, virtualClient);
            state.getGame().updateAll();
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
            state.getGame().updateAll();
        }
    }

    public ControllerState getState() {
        return state;
    }
    public BoardConfigLoader getBoardConfigLoader() {
        return boardConfigLoader;
    }
    public GameDAO getGameDAO() {
        return gameDAO;
    }
}
