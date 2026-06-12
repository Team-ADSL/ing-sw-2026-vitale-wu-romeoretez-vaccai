package org.adsl.shared.network.responses;

import org.adsl.shared.exceptions.InvalidResponseException;

/**
 * Visitor interface for dispatching {@link ServerResponse} subtypes on the
 * client side without instanceof checks. Implemented by {@code AppCoordinator}
 * to route each incoming response to the appropriate screen or handler.
 */
public interface ResponseVisitor {
    /**
     * Handles a {@link ServerPing} heartbeat response.
     *
     * @param response the heartbeat response
     * @throws InvalidResponseException if the response cannot be processed
     */
    void visit(ServerPing response) throws InvalidResponseException;

    /**
     * Handles a {@link LoginNeeded} response requiring the client to authenticate.
     *
     * @param response the login-needed response
     * @throws InvalidResponseException if the response cannot be processed
     */
    void visit(LoginNeeded response) throws InvalidResponseException;

    /**
     * Handles a {@link HomeUpdate} response updating the list of open games.
     *
     * @param response the home-screen update response
     * @throws InvalidResponseException if the response cannot be processed
     */
    void visit(HomeUpdate response) throws InvalidResponseException;

    /**
     * Handles a {@link LobbyUpdate} response updating the lobby roster.
     *
     * @param response the lobby update response
     * @throws InvalidResponseException if the response cannot be processed
     */
    void visit(LobbyUpdate response) throws InvalidResponseException;

    /**
     * Handles a {@link TotemAvailableUpdate} response updating the totems still
     * available for selection.
     *
     * @param response the totem-availability update response
     * @throws InvalidResponseException if the response cannot be processed
     */
    void visit(TotemAvailableUpdate response) throws InvalidResponseException;

    /**
     * Handles a {@link GameUpdate} response carrying a new game-state snapshot.
     *
     * @param response the game update response
     * @throws InvalidResponseException if the response cannot be processed
     */
    void visit(GameUpdate response) throws InvalidResponseException;

    /**
     * Handles a {@link GameEnded} response signalling that the game is over.
     *
     * @param response the game-ended response
     * @throws InvalidResponseException if the response cannot be processed
     */
    void visit(GameEnded response) throws InvalidResponseException;

    /**
     * Handles an {@link ErrorResponse} reporting a rejected request or server error.
     *
     * @param response the error response
     * @throws InvalidResponseException if the response cannot be processed
     */
    void visit(ErrorResponse response) throws InvalidResponseException;

    /**
     * Handles a {@link ServerDisconnected} response signalling a forced disconnection.
     *
     * @param response the disconnection response
     * @throws InvalidResponseException if the response cannot be processed
     */
    void visit(ServerDisconnected response) throws InvalidResponseException;

    /**
     * Handles an {@link EventsTriggered} response for a resolved end-of-round event.
     *
     * @param response the event-triggered response
     * @throws InvalidResponseException if the response cannot be processed
     */
    void visit(EventsTriggered response) throws InvalidResponseException;

    /**
     * Handles a {@link GameLogRestore} response carrying the full game-log transcript.
     *
     * @param response the game-log restore response
     * @throws InvalidResponseException if the response cannot be processed
     */
    void visit(GameLogRestore response) throws InvalidResponseException;
}