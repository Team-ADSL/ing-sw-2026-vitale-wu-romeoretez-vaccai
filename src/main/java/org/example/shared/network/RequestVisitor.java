package org.example.shared.network;

import org.example.shared.exceptions.InvalidMoveException;
import org.example.shared.network.requests.*;

public interface RequestVisitor<T> {
    void visit(CreateGameRequest req, T context) throws InvalidMoveException;
    void visit(ConnectToGameRequest req, T context) throws InvalidMoveException;
    void visit(StartGameRequest req, T context) throws InvalidMoveException;
    void visit(MakeMoveRequest req, T context) throws InvalidMoveException;
    void visit(ClientDisconnected req, T context) throws InvalidMoveException;
}
