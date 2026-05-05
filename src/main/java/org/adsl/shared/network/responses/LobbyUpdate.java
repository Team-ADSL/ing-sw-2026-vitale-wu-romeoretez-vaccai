package org.adsl.shared.network.responses;

import org.adsl.client.exceptions.InvalidResponseException;

import java.util.List;

public class LobbyUpdate extends ServerResponse{
    private final int gameId;
    private final List<String> players;
    private final int numPlayerAllowed;

    public LobbyUpdate(int gameId, List<String> players, int numPlayerAllowed) {
        this.gameId = gameId;
        this.players = players;
        this.numPlayerAllowed = numPlayerAllowed;
    }

    @Override
    public void accept(ResponseVisitor visitor) throws InvalidResponseException {
        visitor.visit(this);
    }

    public int getGameId() {
        return gameId;
    }

    public List<String> getPlayers() {
        return players;
    }

    public int getNumPlayerAllowed() {
        return numPlayerAllowed;
    }
}
