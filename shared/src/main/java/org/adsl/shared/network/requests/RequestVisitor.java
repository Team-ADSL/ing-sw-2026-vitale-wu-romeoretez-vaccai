package org.adsl.shared.network.requests;

import org.adsl.shared.exceptions.ServerException;

/**
 * Visitor interface for dispatching {@link ClientRequest} subtypes without
 * instanceof checks. Implemented by {@code ControllerState} (game-phase
 * dispatch) and {@code ServerController} (lobby-level dispatch).
 *
 * @param <T> the context type passed alongside the request (typically
 *            {@code VirtualClient} on the server side)
 */
public interface RequestVisitor<T> {
    void visit(ClientPing req, T context) throws ServerException;
    void visit(ClientConnection req, T context) throws ServerException;
    void visit(LoginRequest req, T context) throws ServerException;
    void visit(LogoutRequest req, T context) throws ServerException;
    void visit(ExitLobbyRequest req, T context) throws ServerException;
    void visit(CreateGameRequest req, T context) throws ServerException;
    void visit(EnterGameRequest req, T context) throws ServerException;
    void visit(StartGameRequest req, T context) throws ServerException;
    void visit(TotemPickingRequest req, T context) throws ServerException;
    void visit(MoveRequest req, T context) throws ServerException;
    void visit(ExitGameRequest req, T context) throws ServerException;
    void visit(ClientDisconnected req, T context) throws ServerException;
}
