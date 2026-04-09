package org.example.shared.network.requests;

import org.example.server.exceptions.InvalidRequestException;

public interface RequestVisitor<T> {
    void visit(ClientConnection req, T context) throws InvalidRequestException;
    void visit(SetUsernameRequest req, T context) throws InvalidRequestException;
    void visit(CreateGameRequest req, T context) throws InvalidRequestException;
    void visit(EnterGameRequest req, T context) throws InvalidRequestException;
    void visit(StartGameRequest req, T context) throws InvalidRequestException;
    void visit(MakeMoveRequest req, T context) throws InvalidRequestException;
    void visit(ClientDisconnected req, T context) throws InvalidRequestException;
}
