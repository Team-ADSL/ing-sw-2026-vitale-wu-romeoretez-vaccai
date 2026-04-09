package org.example.shared.network.responses;

import org.example.client.exceptions.InvalidResponseException;
import org.example.shared.model.MatchResult;

import java.util.List;

public class GameEnded extends ServerResponse {
    private final List<MatchResult> results;

    public GameEnded(List<MatchResult> results) {
        this.results = results;
    }

    @Override
    public void accept(ResponseVisitor visitor) throws InvalidResponseException {
        visitor.visit(this);
    }

    public List<MatchResult> getResults() {
        return results;
    }
}
