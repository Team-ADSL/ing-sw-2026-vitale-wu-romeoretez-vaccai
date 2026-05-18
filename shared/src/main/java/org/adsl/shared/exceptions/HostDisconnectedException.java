package org.adsl.shared.exceptions;

/**
 * Thrown by {@code LobbyState} when the game host disconnects, signalling that
 * all remaining lobby players must be evicted. Caught by
 * {@code ServerController} to handle host-disconnection cleanup separately from
 * ordinary disconnections.
 */
public class HostDisconnectedException extends ServerException {
    public HostDisconnectedException(String message) {
        super(message);
    }
}
