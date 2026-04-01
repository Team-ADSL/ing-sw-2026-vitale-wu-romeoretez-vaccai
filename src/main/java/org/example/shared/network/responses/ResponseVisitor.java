package org.example.shared.network.responses;

import org.example.shared.exceptions.InvalidResponseException;

public interface ResponseVisitor {
    void visit(SetUsername response) throws InvalidResponseException;
    void visit(HomeUpdate response) throws InvalidResponseException;
    void visit(LobbyUpdate response) throws InvalidResponseException;
    void visit(GameUpdate response) throws InvalidResponseException;
    void visit(GameEnded response) throws InvalidResponseException;
    void visit(ErrorResponse response) throws InvalidResponseException;
    void visit(ServerDisconnected response) throws InvalidResponseException;
}