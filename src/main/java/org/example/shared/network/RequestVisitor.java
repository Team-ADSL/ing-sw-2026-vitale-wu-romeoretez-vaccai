package org.example.shared.network;

import org.example.shared.exceptions.InvalidRequestException;
import org.example.shared.network.requests.*;

public interface RequestVisitor<T> {
    void visit(CreateGameRequest req, T context) throws InvalidRequestException;
    void visit(ConnectToGameRequest req, T context) throws InvalidRequestException;
    void visit(StartGameRequest req, T context) throws InvalidRequestException;
    void visit(MakeMoveRequest req, T context) throws InvalidRequestException;
    void visit(ClientDisconnected req, T context) throws InvalidRequestException;
}
