package org.adsl.shared.network.responses;

import org.adsl.client.exceptions.InvalidResponseException;

import java.util.List;
import java.util.stream.Collectors;

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

    @Override
    public String toString(){
        return activeGames.stream().map(String::valueOf).collect(Collectors.joining(" "));
    }
}
