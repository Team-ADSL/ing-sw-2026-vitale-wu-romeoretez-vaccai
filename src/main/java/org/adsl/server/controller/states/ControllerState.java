package org.adsl.server.controller.states;

import org.adsl.server.controller.GameController;
import org.adsl.server.model.Player;
import org.adsl.server.network.VirtualClient;
import org.adsl.shared.network.requests.RequestVisitor;
import org.adsl.shared.network.requests.*;
import org.adsl.server.exceptions.GameException;
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

    public ControllerState onEntry() throws GameException {
        return this;
    }

    public ControllerState calcNextState(){
        return this;
    }

    // Called only in non-automatic states
    public Player controlIfPlayerTurn(VirtualClient virtualClient) throws GameException {
        if(virtualClient.getClientUsername().isEmpty()){
            throw new GameException("Virtual client has no username associated.");
        }
        Player reqPlayer = getGame().getPlayers().stream()
                .filter(p -> p.getName().equals(virtualClient.getClientUsername().get()))
                .findFirst()
                .orElseThrow(() -> new GameException("Player not in current game"));

        Optional<Player> currentPlayerContainer = getGame().getCurrentPlayer();
        assert currentPlayerContainer.isPresent(); // Setted on creation in every non-automatic state
        if(!currentPlayerContainer.get().equals(reqPlayer) ) {
            throw new GameException("The current player is " + currentPlayerContainer.get().getName());
        }
        return reqPlayer;
    }

    @Override
    public void visit(ClientPing req, VirtualClient virtualClient) throws GameException {}
    @Override
    public void visit(ClientConnection req, VirtualClient virtualClient) throws GameException {
        throw new GameException("New connection rejected.");
    }
    @Override
    public void visit(LoginRequest req, VirtualClient virtualClient) throws GameException {
        throw new GameException("Username setting rejected.");
    }
    @Override
    public void visit(CreateGameRequest req, VirtualClient virtualClient) throws GameException {
        throw new GameException("Create game request rejected.");
    }
    @Override
    public void visit(EnterGameRequest req, VirtualClient virtualClient) throws GameException {
        throw new GameException("Connection rejected.");
    }
    @Override
    public void visit(StartGameRequest req, VirtualClient virtualClient) throws GameException {
        throw new GameException("Start request rejected.");
    }
    @Override
    public void visit(MakeMoveRequest req, VirtualClient virtualClient) throws GameException {
        throw new GameException("Move request rejected.");
    }
    @Override
    public void visit(ClientDisconnected req, VirtualClient virtualClient) throws GameException {
        if(virtualClient.getClientUsername().isEmpty()){
            throw new GameException("Virtual client has no username associated.");
        }
        getGame().getPlayers().stream()
            .filter(p -> p.getName().equals(virtualClient.getClientUsername().get()))
            .findFirst()
            .orElseThrow(() -> new GameException("Player not in current game"))
            .setActive(false);
        getGame().removeVirtualClient(virtualClient);
        virtualClient.setGameId(null);
        toStop = true;
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
    public ControllerState getNextState() {
        return nextState;
    }

    public void setNextState(ControllerState nextState) {
        this.nextState = nextState;
    }
}
