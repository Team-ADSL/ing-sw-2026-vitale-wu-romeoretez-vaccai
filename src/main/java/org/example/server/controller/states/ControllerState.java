package org.example.server.controller.states;

import org.example.server.network.VirtualClient;
import org.example.shared.network.RequestVisitor;
import org.example.shared.network.requests.*;
import org.example.shared.exceptions.InvalidMoveException;
import org.example.server.model.Game;


public abstract class ControllerState implements RequestVisitor<VirtualClient> {
    private final Game game;

    public ControllerState(Game game) {
        this.game = game;
    }

    public ControllerState onEntry(){
        return this;
    }
    public abstract ControllerState nextState();
    @Override
    public void visit(CreateGameRequest req, VirtualClient virtualClient) throws InvalidMoveException {
        throw new InvalidMoveException("Move not allowed in this phase");
    }
    @Override
    public void visit(ConnectToGameRequest req, VirtualClient virtualClient) throws InvalidMoveException {
        throw new InvalidMoveException("Move not allowed in this phase");
    }
    @Override
    public void visit(StartGameRequest req, VirtualClient virtualClient) throws InvalidMoveException {
        throw new InvalidMoveException("Move not allowed in this phase");
    }
    @Override
    public void visit(MakeMoveRequest req, VirtualClient virtualClient) throws InvalidMoveException {
        throw new InvalidMoveException("Move not allowed in this phase");
    }
    @Override
    public void visit(ClientDisconnected req, VirtualClient virtualClient) throws InvalidMoveException {
        throw new InvalidMoveException("Move not allowed in this phase");
    }

    public Game getGame() {
        return game;
    }
}
