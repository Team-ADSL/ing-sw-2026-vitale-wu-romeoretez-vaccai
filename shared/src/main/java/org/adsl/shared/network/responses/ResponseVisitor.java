package org.adsl.shared.network.responses;

import org.adsl.shared.exceptions.InvalidResponseException;

public interface ResponseVisitor {
    void visit(ServerPing response) throws InvalidResponseException;
    void visit(LoginNeeded response) throws InvalidResponseException;
    void visit(HomeUpdate response) throws InvalidResponseException;
    void visit(LobbyUpdate response) throws InvalidResponseException;
    void visit(TotemAvailableUpdate response) throws InvalidResponseException;
    void visit(GameUpdate response) throws InvalidResponseException;
    void visit(GameEnded response) throws InvalidResponseException;
    void visit(ErrorResponse response) throws InvalidResponseException;
    void visit(ServerDisconnected response) throws InvalidResponseException;
    void visit(EventsTriggered response) throws InvalidResponseException;
}