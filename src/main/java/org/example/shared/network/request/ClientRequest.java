package org.example.shared.network.request;

public abstract class ClientRequest {
    private final int gameId;

    public ClientRequest(int gameId) {
        this.gameId = gameId;
    }

    public int getGameId() {
        return gameId;
    }
}
