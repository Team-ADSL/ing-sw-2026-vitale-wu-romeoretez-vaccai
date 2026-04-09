package org.example.shared.network.responses;

import org.example.client.exceptions.InvalidResponseException;

import java.util.List;

public class LobbyUpdate extends ServerResponse{
    private final List<String> players;

    public LobbyUpdate(List<String> players) {
        this.players = players;
    }

    @Override
    public void accept(ResponseVisitor visitor) throws InvalidResponseException {
        visitor.visit(this);
    }

    public List<String> getPlayers() {
        return players;
    }
}
