package org.adsl.server.controller;

import org.adsl.server.config.BoardConfigLoader;
import org.adsl.server.controller.states.ControllerState;
import org.adsl.shared.exceptions.ServerException;
import org.adsl.server.model.Player;
import org.adsl.server.persistence.GameDAO;
import org.adsl.server.network.VirtualClient;
import org.adsl.server.persistence.GamePersistenceManager;
import org.adsl.shared.network.requests.ClientRequest;

import java.util.List;

/**
 * Per-game controller that owns the {@link ControllerState} state machine.
 * <p>
 * All client requests for a single game are routed here by {@link ServerController}.
 * Each call to {@link #handleClientRequest} dispatches the request to the current
 * state via the visitor pattern, then drives automatic state transitions until
 * a manual (waiting) state is reached.
 * </p>
 */
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

    /**
     * Dispatches {@code req} to the current state and drives automatic state
     * transitions until a manual (waiting) state is reached.
     * <p>
     * Synchronized to serialise concurrent requests for the same game.
     * </p>
     *
     * @param req           the request to handle
     * @param virtualClient the client who sent the request
     * @throws ServerException if the current state rejects the request, host exit or a
     *                         state-transition loop is detected
     */
    public synchronized void handleClientRequest(ClientRequest req, VirtualClient virtualClient) throws ServerException {
        req.accept(state, virtualClient);
        changeState(state.getNextState());
    }

    // To handle automatics states
    private void changeState(ControllerState newControllerState) throws ServerException {
        ControllerState nextState = newControllerState;
        int transitionCount = 0;
        final int MAX_TRANSITIONS = 50; // To avoid thread starvation
        while (nextState != state && nextState != null) {
            if (transitionCount++ > MAX_TRANSITIONS) {
                throw new ServerException("[FATAL] Infinite state transition loop detected.");
            }
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

    public List<String> getLobbyPlayers() {
        if (state == null) return List.of();
        return state.getGame().getPlayers().stream()
                .map(Player::getName).toList();
    }

    public int getCapacity() {
        if (state == null) return 0;
        return state.getGame().getNumPlayer();
    }
    public ControllerState getState() { return state; }
}
