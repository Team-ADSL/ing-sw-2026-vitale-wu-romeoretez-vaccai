package org.example.shared.network.requests;

import org.example.server.exceptions.GameException;

public interface RequestVisitor<T> {
    void visit(ClientConnection req, T context) throws GameException;
    void visit(LoginRequest req, T context) throws GameException;
    void visit(CreateGameRequest req, T context) throws GameException;
    void visit(EnterGameRequest req, T context) throws GameException;
    void visit(StartGameRequest req, T context) throws GameException;
    void visit(MakeMoveRequest req, T context) throws GameException;
    void visit(ClientDisconnected req, T context) throws GameException;
}
