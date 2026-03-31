package org.example.shared.network.requests;

import org.example.shared.exceptions.InvalidRequestException;
import org.example.shared.network.RequestVisitor;

import java.io.Serializable;

public abstract class ClientRequest implements Serializable {
    private final int gameId;

    public ClientRequest(int gameId) {
        this.gameId = gameId;
    }

    public abstract <T> void accept(RequestVisitor<T> visitor, T context) throws InvalidRequestException;

    public int getGameId() {
        return gameId;
    }
}
