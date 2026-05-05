package org.adsl.server.controller.states;

import org.adsl.server.controller.GameController;
import org.adsl.server.model.Player;
import org.adsl.server.network.VirtualClient;
import org.adsl.shared.network.requests.RequestVisitor;
import org.adsl.shared.network.requests.*;
import org.adsl.server.exceptions.ServerException;
import org.adsl.server.model.Game;

import java.util.Optional;

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

    public ControllerState onEntry() throws ServerException {
        return this;
    }

    public ControllerState calcNextState(){
        return this;
    }

    // Called only in non-automatic states
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
    public void visit(MoveRequest req, VirtualClient virtualClient) throws ServerException {
        throw new ServerException("Move request rejected.");
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
