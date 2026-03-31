package org.example.server.controller.states;

import org.example.server.controller.GameController;
import org.example.server.model.Player;
import org.example.server.network.VirtualClient;
import org.example.shared.network.RequestVisitor;
import org.example.shared.network.requests.*;
import org.example.shared.exceptions.InvalidRequestException;
import org.example.server.model.Game;

import java.util.Optional;


public abstract class ControllerState implements RequestVisitor<VirtualClient> {
    private final Game game;
    private final GameController context;

    public ControllerState(Game game, GameController context) {
        this.game = game;
        this.context = context;
    }

    public abstract ControllerState onEntry() throws Exception;

    public abstract ControllerState nextState();

    public Player controlIfPlayerTurn(ClientRequest req, VirtualClient virtualClient) throws InvalidRequestException{
        Optional<Player> reqPlayer = getGame().getPlayers().stream()
                .filter(p -> p.getName().equals(virtualClient.getClientUsername()))
                .findFirst();
        if(reqPlayer.isEmpty()){
            throw new InvalidRequestException("Player not in current game");
        }

        Optional<Player> currentPlayerContainer = getGame().getCurrentPlayer();
        assert currentPlayerContainer.isPresent(); // Setted on creation
        if(!currentPlayerContainer.get().equals(reqPlayer.get()) ) {
            throw new InvalidRequestException("The current player is " + currentPlayerContainer.get().getName());
        }

        return reqPlayer.get();
    }

    @Override
    public void visit(ClientConnection req, VirtualClient virtualClient) throws InvalidRequestException {
        throw new InvalidRequestException("New connection not allowed:");
    }
    @Override
    public void visit(SetUsernameRequest req, VirtualClient virtualClient) throws InvalidRequestException {
        throw new InvalidRequestException("You can't change username here:");
    }
    @Override
    public void visit(CreateGameRequest req, VirtualClient virtualClient) throws InvalidRequestException {
        throw new InvalidRequestException("Create game request rejected:");
    }
    @Override
    public void visit(EnterGameRequest req, VirtualClient virtualClient) throws InvalidRequestException {
        throw new InvalidRequestException("Connection rejected:");
    }
    @Override
    public void visit(StartGameRequest req, VirtualClient virtualClient) throws InvalidRequestException {
        throw new InvalidRequestException("Start request rejected:");
    }
    @Override
    public void visit(MakeMoveRequest req, VirtualClient virtualClient) throws InvalidRequestException {
        throw new InvalidRequestException("Move request rejected:");
    }
    @Override
    public void visit(ClientDisconnected req, VirtualClient virtualClient) throws InvalidRequestException {
        throw new InvalidRequestException("Disconnection unespected:");
    }

    public Game getGame() {
        return game;
    }
    public GameController getContext() {
        return context;
    }
}
