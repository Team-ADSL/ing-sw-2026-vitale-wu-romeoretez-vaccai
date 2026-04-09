package org.example.shared.network.responses;

import org.example.client.exceptions.InvalidResponseException;

import java.util.List;

public class HomeUpdate extends ServerResponse {
    private final List<Integer> activeGames;

    public HomeUpdate(List<Integer> activeGames) {
        this.activeGames = activeGames;
    }

    @Override
    public void accept(ResponseVisitor visitor) throws InvalidResponseException {
        visitor.visit(this);
    }

    public List<Integer> getActiveGames() {
        return activeGames;
    }
}
