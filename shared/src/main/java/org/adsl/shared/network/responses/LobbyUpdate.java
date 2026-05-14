package org.adsl.shared.network.responses;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.adsl.shared.exceptions.InvalidResponseException;

import java.util.List;

public class LobbyUpdate extends ServerResponse{
    private final int gameId;
    private final List<String> players;
    private final int numPlayerAllowed;

    @JsonCreator
    public LobbyUpdate(@JsonProperty("gameId") int gameId,
                       @JsonProperty("players") List<String> players,
                       @JsonProperty("numPlayerAllowed") int numPlayerAllowed) {
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
