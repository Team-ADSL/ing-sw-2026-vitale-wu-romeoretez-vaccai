package org.adsl.shared.network.responses;

import org.adsl.client.exceptions.InvalidResponseException;
import org.adsl.shared.model.MatchResult;

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
