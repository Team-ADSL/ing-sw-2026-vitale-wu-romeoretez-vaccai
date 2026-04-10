package org.example.server.controller.states;

import org.example.server.controller.GameController;
import org.example.server.model.Player;
import org.example.server.network.VirtualClient;
import org.example.shared.network.requests.RequestVisitor;
import org.example.shared.network.requests.*;
import org.example.server.exceptions.InvalidRequestException;
import org.example.server.model.Game;

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

    public ControllerState onEntry() throws Exception{
        return this;
    }

    public ControllerState calcNextState(){
        return this;
    }

    // Called only in non-automatic states
    public Player controlIfPlayerTurn(VirtualClient virtualClient) throws InvalidRequestException{
        Player reqPlayer = getGame().getPlayers().stream()
                .filter(p -> p.getName().equals(virtualClient.getClientUsername()))
                .findFirst()
                .orElseThrow(() -> new InvalidRequestException("Player not in current game"));

        Optional<Player> currentPlayerContainer = getGame().getCurrentPlayer();
        assert currentPlayerContainer.isPresent(); // Setted on creation in every non-automatic state
        if(!currentPlayerContainer.get().equals(reqPlayer) ) {
            throw new InvalidRequestException("The current player is " + currentPlayerContainer.get().getName());
        }
        return reqPlayer;
    }

    @Override
    public void visit(ClientConnection req, VirtualClient virtualClient) throws InvalidRequestException {
        throw new InvalidRequestException("New connection rejected.");
    }
    @Override
    public void visit(SetUsernameRequest req, VirtualClient virtualClient) throws InvalidRequestException {
        throw new InvalidRequestException("Username setting rejected.");
    }
    @Override
    public void visit(CreateGameRequest req, VirtualClient virtualClient) throws InvalidRequestException {
        throw new InvalidRequestException("Create game request rejected.");
    }
    @Override
    public void visit(EnterGameRequest req, VirtualClient virtualClient) throws InvalidRequestException {
        throw new InvalidRequestException("Connection rejected.");
    }
    @Override
    public void visit(StartGameRequest req, VirtualClient virtualClient) throws InvalidRequestException {
        throw new InvalidRequestException("Start request rejected.");
    }
    @Override
    public void visit(MakeMoveRequest req, VirtualClient virtualClient) throws InvalidRequestException {
        throw new InvalidRequestException("Move request rejected.");
    }
    @Override
    public void visit(ClientDisconnected req, VirtualClient virtualClient) throws InvalidRequestException {
        getGame().getPlayers().stream()
            .filter(p -> p.getName().equals(virtualClient.getClientUsername()))
            .findFirst()
            .orElseThrow(() -> new InvalidRequestException("Player not in current game"))
            .setActive(false);
        getGame().removeVirtualClient(virtualClient);
        virtualClient.setGameController(null);
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

    public void setToStop(boolean toStop) {
        this.toStop = toStop;
    }
    public void setNextState(ControllerState nextState) {
        this.nextState = nextState;
    }
}
