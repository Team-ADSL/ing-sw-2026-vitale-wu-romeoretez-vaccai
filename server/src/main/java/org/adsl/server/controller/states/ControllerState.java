package org.adsl.server.controller.states;

import org.adsl.server.controller.GameController;
import org.adsl.server.model.Player;
import org.adsl.server.network.VirtualClient;
import org.adsl.shared.network.requests.RequestVisitor;
import org.adsl.shared.network.requests.*;
import org.adsl.shared.exceptions.ServerException;
import org.adsl.server.model.Game;

import java.util.Optional;

/**
 * Base class for all game-phase states in the {@link GameController} state machine.
 * <p>
 * Each concrete state handles the subset of {@link RequestVisitor} methods that
 * are valid in its phase; all others throw {@link ServerException} by default.
 * </p>
 * <ul>
 *   <li><b>Manual states</b> – wait for a player request before transitioning
 *       (e.g. {@link LobbyState}, {@link TotemPlacementState}).</li>
 *   <li><b>Automatic states</b> – transition immediately on {@link #onEntry}
 *       (e.g. {@link InitGameState}, {@link EndRoundState}).</li>
 * </ul>
 * <p>
 * After each request, {@link GameController} calls {@link #getNextState()} and
 * keeps calling {@link #onEntry()} on new states until a manual state returns
 * {@code this}.
 * </p>
 */
public abstract class ControllerState implements RequestVisitor<VirtualClient> {
    private final Game game;
    private final GameController context;
    private ControllerState nextState;
    private boolean toStop;

    public ControllerState(Game game, GameController context) {
        this.game = game;
        this.context = context;
        this.toStop = false;
        this.nextState = this;
    }

    /**
     * Called when the state machine enters this state. Automatic states perform
     * their logic here and return the next state; manual states simply return
     * {@code this} and wait for incoming requests.
     *
     * @return the next state to transition to, or {@code this} to stay
     * @throws ServerException if entry-time logic fails
     */
    public ControllerState onEntry() throws ServerException {
        return this;
    }

    /**
     * Computes which state should follow the current one after the last request
     * was processed. Overridden by concrete states that have transition logic.
     *
     * @return the next {@link ControllerState}, or {@code this} if no transition
     */
    public ControllerState calcNextState(){
        return this;
    }

    /**
     * Validates that the requesting client is the current player and returns
     * their {@link Player} object. Only meaningful in manual (non-automatic) states.
     *
     * @param virtualClient the client sending the request
     * @return the {@link Player} whose turn it is
     * @throws ServerException if the client has no username, is not in this game,
     *                         or is not the current player
     */
    public Player controlIfPlayerTurn(VirtualClient virtualClient) throws ServerException {
        if(virtualClient.getClientUsername().isEmpty()){
            throw new ServerException("Virtual client has no username associated.");
        }
        Player reqPlayer = getGame().getPlayers().stream()
                .filter(p -> p.getName().equals(virtualClient.getClientUsername().get()))
                .findFirst()
                .orElseThrow(() -> new ServerException("Player not in current game"));

        Player currentPlayer = getGame().getCurrentPlayer()
                .orElseThrow(() -> new ServerException("[FATAL] Internal Error: No current player found in this state"));

        if (!currentPlayer.equals(reqPlayer)) {
            throw new ServerException("The current player is " + currentPlayer.getName());
        }
        return reqPlayer;
    }

    @Override
    public void visit(ClientPing req, VirtualClient virtualClient) throws ServerException {}

    @Override
    public void visit(ClientConnection req, VirtualClient virtualClient) throws ServerException {
        throw new ServerException("New connection rejected.");
    }

    @Override
    public void visit(LoginRequest req, VirtualClient virtualClient) throws ServerException {
        throw new ServerException("Username setting rejected.");
    }

    @Override
    public void visit(LogoutRequest req, VirtualClient virtualClient) throws ServerException {
        throw new ServerException("Logout not allowed in game.");
    }

    @Override
    public void visit(ExitLobbyRequest req, VirtualClient virtualClient) throws ServerException {
        throw new ServerException("Exit not allowed in this phase.");
    }

    @Override
    public void visit(CreateGameRequest req, VirtualClient virtualClient) throws ServerException {
        throw new ServerException("Create game request rejected.");
    }

    @Override
    public void visit(EnterGameRequest req, VirtualClient virtualClient) throws ServerException {
        throw new ServerException("Connection rejected.");
    }

    @Override
    public void visit(StartGameRequest req, VirtualClient virtualClient) throws ServerException {
        throw new ServerException("Start request rejected.");
    }

    @Override
    public void visit(TotemPickingRequest req, VirtualClient virtualClient) throws ServerException {
        throw new ServerException("Totem picking rejected.");
    }

    @Override
    public void visit(MoveRequest req, VirtualClient virtualClient) throws ServerException {
        throw new ServerException("Move request rejected.");
    }

    @Override
    public void visit(ExitGameRequest req, VirtualClient virtualClient) throws ServerException {
        throw new ServerException("Exit game request rejected.");
    }

    @Override
    public void visit(ClientDisconnected req, VirtualClient virtualClient) throws ServerException {
        if(virtualClient.getClientUsername().isEmpty()){
            return;
        }
        getGame().getPlayers().stream()
            .filter(p -> p.getName().equals(virtualClient.getClientUsername().get()))
            .findFirst()
            .orElseThrow(() -> new ServerException("Player not in current game"))
            .setActive(false);
        getGame().removeVirtualClient(virtualClient);
        System.out.println("[DISCONNECTION] Removing " + virtualClient.getClientUsername()
        + " from game " + getGame().getGameId());
        toStop = true;
        setNextState(calcNextState());
    }

    public Game getGame() {
        return game;
    }
    public GameController getContext() {
        return context;
    }
    public boolean isToStop() {
        return toStop;
    }

    public void setToStop(boolean toStop) {
        this.toStop = toStop;
    }

    public ControllerState getNextState() {
        return nextState;
    }

    public void setNextState(ControllerState nextState) {
        this.nextState = nextState;
    }
}
