package org.example.shared.network.requests;

import org.example.shared.exceptions.InvalidMoveException;
import org.example.shared.network.RequestVisitor;

public abstract class ClientRequest {
    private final int gameId;
    private final String username;

    public ClientRequest(int gameId, String username) {
        this.gameId = gameId;
        this.username = username;
    }

    public abstract <T> void accept(RequestVisitor<T> visitor, T context) throws InvalidMoveException;

    public int getGameId() {
        return gameId;
    }

    public String getUsername() {
        return username;
    }
}
